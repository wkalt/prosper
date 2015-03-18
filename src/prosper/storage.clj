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
     :amount_remaining AmountRemaining
     :amount_participation AmountParticipation
     :listing_amount_funded ListingAmountFunded
     :listingnumber ListingNumber}))

(defn mapvals
  ([f m]
   (into {} (for [[k v] m] [k (f v)])))
  ([f ks m]
   (reduce (fn [m k] (update-in m [k] f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn store-events!
  [listings]
  (apply (partial jdbcd/insert-records :events)
         (map munge-event listings))
  (log/info "stored events"))

(defn existing-events
  [table column values db]
  (let [query (format "select %s from %s where %s in (%s)"
                      column table column (string/join "," values))]
    (map :listingnumber (jdbc/query db query))))

(defn store-listings
  [listings-to-store]
    (->> listings-to-store
         (map #(select-keys % (conj (keys numeric-fields) :ListingNumber)))
         (apply (partial jdbcd/insert-records :numeric)))
    (->> listings-to-store
         (map #(select-keys % (conj (keys character-fields) :ListingNumber)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :character))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (let [new-events (map :ListingNumber listings)
           existing (existing-events "numeric" "listingnumber" new-events db)
           events-to-store (set/difference (set new-events) (set existing))
           listings-to-store (->> listings
                                  (filter (comp events-to-store :ListingNumber)))]
       (if (empty? listings-to-store)
         (log/info "no new listings")
         (do (store-listings listings-to-store)
             (log/info (format "stored %s new listings"
                               (count listings-to-store)))))
       (when store-events?
         (store-events! listings))))))
