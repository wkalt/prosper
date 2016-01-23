(ns prosper.storage
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now minutes plus]]
            [clj-time.format :as f]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.set :as set]
            [cheshire.core :as json]
            [clojure.string :as string]
            [prosper.fields :refer [v1-fields date-fields]]
            [clj-time.coerce :refer [to-timestamp]]))

(def release-end-time (atom (now)))

(defn mapvals
  ([f m] (into {} (for [[k v] m] [k (f v)])))
  ([f ks m] (reduce (fn [m k] (update m k f)) m ks)))

(defn update-time-fields
  [listing]
  (mapvals to-timestamp (keys date-fields) listing))

(defn update-events!
  [events]
  (try
    (let [query "INSERT INTO events
                 (listing_number, timestamp, amount_participation,
                 amount_remaining, amount_funded)
                 VALUES %s ON CONFLICT DO NOTHING"
          n (->> events
                 (string/join ",")
                 (format query)
                 (jdbcd/do-commands)
                 first)]
      (if (> n 0)
        (log/infof "Inserted %s new events" n)
        (log/debug "No new events")))
    (catch Exception e
      (log/errorf "%s unravelled exception %s" e (.getNextException e)))))

(defn format-event-rows
  "This can go once jdbc supports insert on conflict."
  [{:keys [amount_remaining amount_participation
           amount_funded listing_number last_updated_date]}]
  (format "(%s, '%s', %s, %s, %s)"
          listing_number
          last_updated_date
          amount_participation
          amount_remaining
          amount_funded))

(defn store-events!
  [listings]
  (update-events! (map format-event-rows listings)))

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
         (map #(select-keys % (keys v1-fields)))
         (map update-time-fields)
         (apply (partial jdbcd/insert-records :listings)))
    (catch Exception e
      (log/errorf "Error storing listings: %s unravelled exception %s"
                  e (.getNextException e)))))

(defn store-listings!
  "must be called within a db connection"
  [db listings]
  (when-let [new-listings (seq (map :listing_number listings))]
    (jdbcd/with-connection db
      (jdbc/with-db-transaction [connection db]
        (let [existing-listings (existing-entries "listings" "listing_number"
                                                  new-listings db)
              new-listing-numbers (set/difference (set new-listings)
                                                  (set existing-listings))
              listings-to-store (filter (comp new-listing-numbers :listing_number)
                                        listings)]
          (if (empty? listings-to-store)
            (log/debug "no new listings")
            (do
              (reset! release-end-time (plus (now) (minutes 15)))
              (store-listings listings-to-store)
              (log/info (format "stored %s new listings"
                                (count listings-to-store)))))
          (store-events! listings))))))

(defn parse-order-date
  [stamp]
  (to-timestamp
    (f/parse (f/formatter "yyyy-MM-dd HH:mm:ss Z") stamp)))

(defn store-investment!
  [response db]
  (jdbcd/with-connection db
    (jdbc/with-db-transaction [connection db]
      (let [row (-> response
                    (update :bid_requests json/generate-string)
                    (update :order_date (comp to-timestamp parse-order-date)))]
        (jdbcd/insert-record :investments row))
      (log/infof "stored investment %s" (:order_id response)))))
