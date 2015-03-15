(ns prosper.storage
  (:require [prosper.query :as query]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [clojure.string :as string]
            [prosper.query :as q]
            [prosper.fields :refer [numeric-fields character-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(defn store-events!
  [listings]
  (let [current-time (now)]
    (apply (partial jdbcd/insert-records :entries)
           (map
             (fn [{:keys [AmountRemaining AmountParticipation
                          ListingAmountFunded ListingNumber]}]
               {:timestamp (to-timestamp current-time)
                :amount_remaining AmountRemaining
                :amount_participation AmountParticipation
                :listing_amount_funded ListingAmountFunded
                :listingnumber ListingNumber}) listings))
    (log/info "stored events")))

(defn existent-entries
  [table column values db]
  (let [query (format "select %s from %s where %s in (%s)"
                      column table column (string/join "," values))]
    (map :listingnumber (jdbc/query db query))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (let [new-entries (map :ListingNumber listings)
           existing-entries (existent-entries "numeric" "listingnumber" new-entries db)
           entries-to-store (set/difference (set new-entries) (set existing-entries))
           listings-for-storage (->> listings
                                     (filter (comp entries-to-store :ListingNumber)))]

       (apply (partial jdbcd/insert-records :numeric)
              (->> listings-for-storage
                   (map #(select-keys % (conj (keys numeric-fields) :ListingNumber)))))

       (apply (partial jdbcd/insert-records :character)
              (->> listings-for-storage
                   (map #(select-keys % (conj (keys character-fields) :ListingNumber)))))

       (log/info "stored listings")
       (when store-events?
         (store-events! listings))))))
