(ns prosper.migrate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clj-time.core :refer [now]]
            [clojure.set :refer [difference]]
            [clj-time.coerce :refer [to-timestamp]]
            [clojure.tools.logging :as log]
            [prosper.fields :refer [legacy-fields legacy->v1-conversions cb-fields]]
            [clojure.string :refer [lower-case]]
            [clojure.walk :refer [stringify-keys]]
            [environ.core :refer [env]]
            [prosper.query :as q]))

(defn rename-column
  [t [x y]]
  (when (not= (lower-case (name x)) (lower-case y))
    (format "ALTER TABLE %s RENAME COLUMN %s TO %s" t (name x) y)))

(defn initial-migration
  []
  (jdbcd/do-commands
    (apply jdbcd/create-table-ddl :listings
           (-> legacy-fields
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

(defn migrate-to-v1-fields
  []
  (apply jdbcd/do-commands
         (filter (comp not nil?)
                 (map (partial rename-column "listings")
                      legacy->v1-conversions)))

  (jdbcd/do-commands
    "ALTER TABLE events RENAME COLUMN listingnumber TO listing_number"
    "ALTER TABLE events RENAME COLUMN amountremaining TO amount_remaining"
    "ALTER TABLE events RENAME COLUMN listing_amount_funded TO amount_funded"))

(defn drop-amountremaining
  []
  (jdbcd/do-commands
    "DROP TABLE amountremaining"))

(defn create-investments-table
  []
  (jdbcd/create-table
    :investments
    ["order_id" "varchar(40) unique not null primary key"]
    ["bid_requests" "text"]
    ["effective_yield" "numeric"]
    ["estimated_loss" "numeric"]
    ["estimated_return" "numeric"]
    ["order_status" "text"]
    ["source" "text"]
    ["order_date" "timestamp not null"])
  (jdbcd/do-commands
    "create index investment_order_date_idx on investments(order_date)"))

(def migrations
  {1 initial-migration
   2 migrate-to-v1-fields
   3 drop-amountremaining
   4 create-investments-table
   })

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
  [db]
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
      (log/info "There are no pending migrations"))))
