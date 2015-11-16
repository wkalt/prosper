(ns prosper.storage
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [clojure.string :as string]
            [prosper.fields :refer [fields date-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(defn mapvals
  ([f m] (into {} (for [[k v] m] [k (f v)])))
  ([f ks m] (reduce (fn [m k] (update-in m [k] f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn insert-statement
  [event]
  (format "(%s,'%s',%s,%s,%s)"
          (:listingnumber event)
          (:timestamp event)
          (:amount_participation event)
          (:amountremaining event)
          (:listing_amount_funded event)))

(defn update-events!
  [events]
  (try
    (let [n (first (jdbcd/do-commands
                     (format "INSERT INTO events
                              (listingnumber,timestamp,amount_participation,
                              amountremaining,listing_amount_funded)
                              VALUES
                              %s ON CONFLICT DO NOTHING"
                             (string/join "," (map insert-statement events)))))]

      (if (> n 0)
        (log/infof "Inserted %s new events" n)))
    (catch Exception e
      (log/errorf "%s unravelled exception %s" e (.getNextException e)))))

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
    (update-events! events-for-storage)))

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
         (map #(select-keys % (conj (keys fields) :ListingNumber)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :listings)))
    (catch Exception e (log/error (format "Error storing listings:"
                                          (.getMessage e))))))

(defn store-listings!
  "must be called within a db connection"
  ([db listings]
   (store-listings! db listings true))
  ([db listings store-events?]
   (jdbc/with-db-transaction [connection db]
     (when-let [new-listings (seq (map :ListingNumber listings))]
       (let [existing-listings (existing-entries "listings" "listingnumber"
                                                 new-listings db)
             new-listingnumbers (set/difference (set new-listings)
                                                (set existing-listings))
             listings-to-store (filter (comp new-listingnumbers :ListingNumber)
                                       listings)]
       (if (empty? listings-to-store)
         (log/debug "no new listings")
         (do (store-listings listings-to-store)
             (log/info (format "stored %s new listings"
                               (count listings-to-store)))))
       (when store-events?
         (store-events! listings db)))))))
