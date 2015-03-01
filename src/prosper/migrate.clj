(ns prosper.migrate
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [prosper.query :as q]))

(def postgres-db {:subprotocol "postgresql"
                  :subname "//localhost:5432/prosper"
                  :user "prosper"
                  :password "prosper"})

(def listings (first (q/query-get "Listings")))

(def numeric-fields
  (into #{} (keys (filter #(number? (val %)) listings))))

(def character-fields
  (into #{} (keys (filter #(string? (val %)) listings))))

(defn initial-migration
  []
  (jdbcd/with-connection postgres-db
    (try

      (jdbcd/do-commands
        (apply jdbcd/create-table-ddl :numeric
               (cons ["listing_number" "bigint UNIQUE PRIMARY KEY NOT NULL"]
                     (seq (zipmap (map name numeric-fields)
                                  (repeat (count numeric-fields) "double precision"))))))

      (jdbcd/do-commands
        (apply jdbcd/create-table-ddl :character
               (cons ["listing_number"
                      "bigint unique primary key references numeric(listing_number)"]
                     (seq (zipmap (map name character-fields)
                                  (repeat (count character-fields) "VARCHAR(120)"))))))

      (jdbcd/do-commands
        "CREATE SEQUENCE entry_id_seq CYCLE")

      (jdbcd/create-table :entries
                          ["entry_id" "bigint NOT NULL PRIMARY KEY DEFAULT nextval('entry_id_seq')"]
                          ["listing_number" "bigint references numeric(listing_number)"]
                          ["timestamp" "TIMESTAMP WITH TIME ZONE"]
                          ["amount_remaining" "integer"]
                          ["amount_participation" "integer"]
                          ["listing_amount_funded" "integer"])


      (catch Exception e (.getNextException e)))))

(defn migrate!
  []
  (do
    (initial-migration)))

#_ (migrate!)
