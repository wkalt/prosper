(ns prosper.storage
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [cheshire.core :as json]
            [clojure.string :as string]
            [prosper.fields :refer [v1-fields date-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(defn mapvals
  ([f m] (into {} (for [[k v] m] [k (f v)])))
  ([f ks m] (reduce (fn [m k] (update-in m [k] f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn insert-event-statement
  [{:keys [listing_number timestamp amount_participation
           amount_remaining amount_funded]}]
  (format "(%s,'%s',%s,%s,%s)"
          listing_number
          timestamp
          amount_participation
          amount_remaining
          amount_funded))

(defn update-events!
  [events]
  (try
    (let [query "INSERT INTO events
                 (listing_number, timestamp, amount_participation,
                 amount_remaining, amount_funded)
                 VALUES %s ON CONFLICT DO NOTHING"
          n (->> (map insert-event-statement events)
                 (string/join ",")
                 (format query)
                 (jdbcd/do-commands)
                 first)]
      (if (> n 0)
        (log/infof "Inserted %s new events" n)
        (log/debug "No new events")))
    (catch Exception e
      (log/errorf "%s unravelled exception %s" e (.getNextException e)))))

(defn munge-event
  [{:keys [amount_remaining amount_participation
           amount_funded listing_number last_updated_date]}]
  ;; this needs to change when last_updated_date is available
  {:timestamp (to-timestamp (now))
   :amount_participation amount_participation
   :amount_funded amount_funded
   :amount_remaining amount_remaining
   :listing_number listing_number})

(defn store-events!
  "This function is not done"
  [listings]
  (update-events! (map munge-event listings)))

(defn existing-entries
  [table column values db]
  (let [query (format "select %s from %s where %s in (%s)"
                      column table column (string/join "," values))]
    (map :listing_number (jdbc/query db query))))

(defn store-listings
  [listings-to-store]
  (log/debug "storing listings")
  (try
    (->> listings-to-store
         (map #(select-keys % (conj (keys v1-fields) :listing_number)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :listings)))
    (catch Exception e
      (log/errorf "Error storing listings: %s unravelled exception %s"
                  e (.getNextException e)))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (when-let [new-listings (seq (map :listing_number listings))]
       (let [existing-listings (existing-entries "listings" "listing_number"
                                                 new-listings db)
             new-listing_numbers (set/difference (set new-listings)
                                                (set existing-listings))
             listings-to-store (filter (comp new-listing_numbers :listing_number)
                                       listings)]
         (if (empty? listings-to-store)
           (log/debug "no new listings")
           (do (store-listings listings-to-store)
               (log/info (format "stored %s new listings"
                                 (count listings-to-store)))))
         (when store-events?
           (store-events! listings)))))))

;(defn store-investment!
;  [response db]
;  (jdbcd/with-connection db
;    (jdbc/with-db-transaction [connection db]
;      (-> response
;          (update-in [:bid_requests] json/generate-string)
;          (update-in [:order_date] to-timestamp)
;          #(jdbcd/insert-record :investments %))
;      (log/infof "stored investment %s" (:order_id response)))))
