(ns prosper.migrate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clj-time.core :refer [now]]
            [clojure.set :refer [difference]]
            [clj-time.coerce :refer [to-timestamp]]
            [clojure.tools.logging :as log]
            [prosper.fields :refer [fields]]
            [clojure.walk :refer [stringify-keys]]
            [prosper.config :refer [*config*]]
            [prosper.query :as q]))

(defn initial-migration
  []

  (jdbcd/do-commands
    (apply jdbcd/create-table-ddl :listings
           (-> fields
               (assoc "listingnumber" "bigint not null primary key")
               stringify-keys
               seq)))
  (jdbcd/do-commands
    "CREATE SEQUENCE entry_id_seq CYCLE")

  (jdbcd/create-table
    :events
    ["entry_id" "bigint NOT NULL PRIMARY KEY DEFAULT nextval('entry_id_seq')"]
    ["listingnumber" "bigint references listings(listingnumber)"]
    ["timestamp" "TIMESTAMP WITH TIME ZONE"]
    ["amount_participation" "numeric"]
    ["amountremaining" "numeric"]
    ["listing_amount_funded" "numeric"])

  (jdbcd/do-commands
    "ALTER TABLE events ADD CONSTRAINT events_listingnumber_amountremaining
     UNIQUE (listingnumber,amountremaining)")

  (jdbcd/create-table
    :amountremaining
    ["listingnumber" "bigint references listings(listingnumber)"]
    ["amount_remaining" "integer not null"])

  (jdbcd/create-table
    :migrations
    ["migration" "integer not null primary key"]
    ["time" "timestamp not null"]))

(def migrations
  {1 initial-migration})

(defn record-migration!
  [migration]
  {:pre [(integer? migration)]}
  (jdbcd/do-prepared
    "INSERT INTO migrations (migration, time) VALUES (?, ?)"
    [migration (to-timestamp (now))]))

(defn applied-migrations
  [db]
  {:post  [(sorted? %)
           (set? %)
           (apply < 0 %)]}
  (try
    (let [query "SELECT migration FROM migrations ORDER BY migration"
          results (jdbcd/transaction (jdbc/query db query))]
      (apply sorted-set (map :migration results)))
    (catch java.sql.SQLException e
      (sorted-set))))

(defn pending-migrations
  [db]
  {:post [(map? %)
          (sorted? %)
          (apply < 0 (keys %))
          (<= (count %) (count migrations))]}
  (try (let [pending (remove (applied-migrations db) (set (keys migrations)))]
         (into (sorted-map)
               (select-keys migrations pending)))
       (catch Exception e (into (sorted-map) migrations))))

(defn migrate!
  "Migrates database to the latest schema version. Does nothing if database is
   already at the latest schema version."
  []
  (let [db (:database *config*)]
    (jdbcd/with-connection db
      (if-let [unexpected (-> (applied-migrations db)
                              (difference (set (keys migrations)))
                              first)]
        (-> "Your database contains an unrecognized migration numbered %s."
            (format unexpected)
            IllegalStateException.
            throw))

      (if-let [pending (seq (pending-migrations db))]
        (jdbcd/transaction
          (doseq [[version migration] pending]
            (log/info (format "Applying database migration version %d" version))
            (try
              (migration)
              (record-migration! version)
              (catch java.sql.SQLException e
                (log/error e "Caught SQLException during migration")
                (let [next (.getNextException e)]
                  (when-not (nil? next)
                    (log/error next "Unravelled exception")))
                (System/exit 1)))))
        (log/info "There are no pending migrations")))))
