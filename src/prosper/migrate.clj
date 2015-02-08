(ns prosper.migrate
  (:require [clojure.java.jdbc :as jdbc]
            [prosper.query :as q]))

(def postgres-db {:subprotocol "postgresql"
                  :subname "//localhost:5432/prosper"
                  :user "prosper"
                  :password "prosper"})


(defn initial-migration
  []
  (jdbc/with-connection postgres
    (create-table :investment_type
                  ["type" "VARCHAR(10)"]
                  ["id" "integer"])
    (create-table :listing_title
                  ["title" "VARCHAR(255)"]
                  ["id" "integer"])
    (create-table :entries
                  ["entry_id" "bigint"]
                  ["listing_id" "bigint"]
                  ["timestamp" "TIMESTAMP WITH TIMEZONE"]
                  ["amount_remaining" "integer"]
                  ["amount_participation" "integer"]
                  ["listing_amount_funded" "integer"])
    (create-table :bureau_fields)))


(def fields (into #{} (keys (first (q/query-get "Listings")))))

(clojure.set/difference fields bureau-fields)

(println fields)

(def bureau-fields
  (into #{} (filter #(re-matches #"[A-Z][A-Z][A-Z]\d\d\d" (name %)) fields)))

(def ALL-fields
  (filter #(re-matches #"AAA\d\d\d" (name %)) fields))

(def ILN-fields
  (filter #(re-matches #"ILN\d\d\d" (name %)) fields))

(println bureau-fields)

(count bureau-fields)


