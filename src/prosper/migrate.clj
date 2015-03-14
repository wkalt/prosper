(ns prosper.migrate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clj-time.core :refer [now]]
            [clojure.set :refer [difference]]
            [clj-time.coerce :refer [to-timestamp]]
            [clojure.tools.logging :as log]
            [prosper.fields :refer [numeric-fields character-fields]]
            [clojure.walk :refer [stringify-keys]]
            [prosper.config :refer [db]]
            [prosper.query :as q]))

(defn initial-migration
  []
  (jdbcd/with-connection db
    (jdbcd/do-commands
      (apply jdbcd/create-table-ddl :numeric
             (seq (stringify-keys (assoc numeric-fields
                                    "listingnumber" "bigint not null primary key")))))
    (jdbcd/do-commands
      (apply jdbcd/create-table-ddl
             :character
             (seq (stringify-keys
                    (assoc  character-fields
                      "listingnumber" "bigint references numeric(listingnumber)")))))
    (jdbcd/do-commands
      "CREATE SEQUENCE entry_id_seq CYCLE")

    (jdbcd/create-table
      :entries
      ["entry_id" "bigint NOT NULL PRIMARY KEY DEFAULT nextval('entry_id_seq')"]
      ["listingnumber" "bigint references numeric(listingnumber)"]
      ["timestamp" "TIMESTAMP WITH TIME ZONE"]
      ["amount_remaining" "integer"]
      ["amount_participation" "integer"]
      ["listing_amount_funded" "integer"])

    (jdbcd/create-table
      :migrations
      ["migration" "integer not null primary key"]
      ["time" "timestamp not null"])))

(def migrations
  {1 initial-migration})

(defn record-migration!
  [migration]
  {:pre [(integer? migration)]}
  (jdbcd/do-prepared
    "INSERT INTO migrations (migration, time) VALUES (?, ?)"
    [migration (to-timestamp (now))]))

(defn applied-migrations
  []
  {:post  [(sorted? %)
           (set? %)
           (apply < 0 %)]}
  (try
    (let [query   "SELECT migration FROM migrations ORDER BY migration"
          results (jdbcd/transaction (jdbc/query db query))]
      (apply sorted-set (map :migration results)))
    (catch java.sql.SQLException e
      (sorted-set))))

(defn pending-migrations
  []
  {:post [(map? %)
          (sorted? %)
          (apply < 0 (keys %))
          (<= (count %) (count migrations))]}
  (try (let [pending (remove (applied-migrations) (set (keys migrations)))]
         (into (sorted-map)
               (select-keys migrations pending)))
       (catch Exception e (into (sorted-map) migrations))))


(jdbcd/with-connection db (pending-migrations))

(defn migrate!
  "Migrates database to the latest schema version. Does nothing if database is
   already at the latest schema version."
  []
  (jdbcd/with-connection db
    (if-let [unexpected (first (difference (applied-migrations) (set (keys migrations))))]
      (throw (IllegalStateException.
               (format "Your database contains an unrecognized schema migration numbered %d."
                       unexpected))))

    (if-let [pending (seq (pending-migrations))]
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
      (log/info "There are no pending migrations"))))
