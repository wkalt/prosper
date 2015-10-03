(ns prosper.storage
  (:require [prosper.query :as query]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [clojure.string :as string]
            [prosper.query :as q]
            [prosper.fields :refer [numeric-fields character-fields date-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(defn munge-event
  [{:keys [AmountRemaining AmountParticipation
           ListingAmountFunded ListingNumber]}]
  (let [current-time (now)]
    {:timestamp (to-timestamp current-time)
     :amount_participation AmountParticipation
     :listing_amount_funded ListingAmountFunded
     :amountremaining AmountRemaining
     :listingnumber ListingNumber}))

(defn mapvals
  ([f m]
   (into {} (for [[k v] m] [k (f v)])))
  ([f ks m]
   (reduce (fn [m k] (update-in m [k] f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn update-event!
  [{:keys [amountremaining listingnumber amount_participation listing_amount_funded] :as event}]
  (jdbcd/insert-records :events (dissoc event :amountremaining))
  (jdbcd/update-or-insert-values :amountremaining
                                 (format "listingnumber=%s" listingnumber)
                                 {:listingnumber listingnumber
                                  :amountremaining amountremaining}))

(defn store-events!
  [listings]
  (let [listingnumbers (map :ListingNumber listings)
        amounts-remaining (->> listingnumbers
                               (format "select listingnumber,amountremaining from
                                        amountremaining where listingnumber in %s" listingnumbers)
                               (reduce #(assoc %1 (:listingnumber %2)
                                          (:amountremaining %2)) {}))
        listings-to-store (if (nil? (first (vals amounts-remaining))) listings
                            (-> listings
                                (filter #(not= (:AmountRemaining %)
                                               (get amounts-remaining (:ListingNumber %))))))]

    (if-not (empty? listings-to-store)
      (do (println "COUNT LISTINGS" (count listings-to-store))
          (map update-event! (map munge-event listings-to-store))
          (log/info "stored new events"))
      (log/info "no new events"))))

(defn existing-entries
  [table column values db]
  (let [query (format "select %s from %s where %s in (%s)"
                      column table column (string/join "," values))]
    (map :listingnumber (jdbc/query db query))))

(defn store-listings
  [listings-to-store]
  (log/debug "storing listings")
  (try
    (->> listings-to-store
         (map #(select-keys % (conj (keys numeric-fields) :ListingNumber)))
         (apply (partial jdbcd/insert-records :numeric)))
    (->> listings-to-store
         (map #(select-keys % (conj (keys character-fields) :ListingNumber)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :character)))
    (catch Exception e (log/error (format "Error storing listings:" (.getMessage e))))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (let [new-listings (map :ListingNumber listings)
           existing-listings (existing-entries "numeric" "listingnumber" new-listings db)
           new-listingnumbers (set/difference (set new-listings) (set existing-listings))
           listings-to-store (->> listings
                                  (filter (comp new-listingnumbers :ListingNumber)))]
       (if (empty? listings-to-store)
         (log/info "no new listings")
         (do (store-listings listings-to-store)
             (log/info (format "stored %s new listings"
                               (count listings-to-store)))))
       (when store-events?
         (store-events! listings))))))
