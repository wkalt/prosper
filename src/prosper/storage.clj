(ns prosper.storage
  (:require [prosper.query :as query]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [clojure.string :as string]
            [prosper.query :as q]
            [prosper.fields :refer [numeric-fields character-fields
                                    date-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(defn mapvals
  ([f m] (into {} (for [[k v] m] [k (f v)])))
  ([f ks m] (reduce (fn [m k] (update-in m [k] f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn update-events!
  [events]
  (try
    (apply (partial jdbcd/insert-records :events) events)
    (catch Exception _
      (println "foo"))))

(defn munge-event
  [{:keys [AmountRemaining AmountParticipation
           ListingAmountFunded ListingNumber] :as event}]
  (let [current-time (now)]
    {:timestamp (to-timestamp current-time)
     :amount_participation AmountParticipation
     :listing_amount_funded ListingAmountFunded
     :amountremaining AmountRemaining
     :listingnumber ListingNumber}))

(defn store-events!
  "This function is not done"
  [listings db]
  (let [events-for-storage (map munge-event listings)]
    (update-events! events-for-storage)
    (log/debug "stored events")))

(defn existing-entries
  [table column values db]
  (let [query (format "select %s from %s where %s in (%s)"
                      column table column (string/join "," values))]
    (map :listingnumber (jdbc/query db query))))

(defn store-listings
  [listings-to-store]
  (log/debug "storing listings")
  ;; TODO make this in a single transaction
  (try
    (->> listings-to-store
         (map #(select-keys % (conj (keys numeric-fields) :ListingNumber)))
         (apply (partial jdbcd/insert-records :numeric)))
    (->> listings-to-store
         (map #(select-keys % (conj (keys character-fields) :ListingNumber)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :character)))
    (catch Exception e (log/error (format "Error storing listings:"
                                          (.getMessage e))))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (let [new-listings (map :ListingNumber listings)
           existing-listings (existing-entries "numeric" "listingnumber"
                                               new-listings db)
           new-listingnumbers (set/difference (set new-listings)
                                              (set existing-listings))
           listings-to-store (filter (comp new-listingnumbers :ListingNumber)
                                     listings)]
       (if (empty? listings-to-store)
         (log/info "no new listings")
         (do (store-listings listings-to-store)
             (log/info (format "stored %s new listings"
                               (count listings-to-store)))))
       (when store-events?
         (store-events! listings db))))))
