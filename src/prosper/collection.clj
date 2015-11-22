(ns prosper.collection
  (:require [prosper.query :as query]
            [clojure.data :refer [diff]]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now plus] :as t]
            [clj-time.predicates :as pr]
            [clj-time.local :as l]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [prosper.storage :as storage]
            [prosper.config :refer [*db* *release-rate* *base-rate*
                                    *storage-threads*]]
            [clojure.core.async :as as]))

(def market-state (atom {}))

(defn release-times
  []
  ;; TODO this is hardcoded
  (if (pr/weekend? (l/local-now))
    [(t/interval (t/today-at 19 50) (t/today-at 20 15))]
    [(t/interval (t/today-at 16 50) (t/today-at 17 15))
     (t/interval (t/today-at 23 50) ;; this one spans midnight UTC
                 (plus (t/today-at 23 50) (t/minutes 25)))]))

(defn in-release?
  []
  (boolean (some true? (map #(t/within? % (now)) (release-times)))))

(defn start-async-producer
  [future-ch]
  (as/thread
    (while true
      (Thread/sleep (if (in-release?) *release-rate* *base-rate*))
      (as/>!! future-ch (query/kit-get "Listings")))))

(defn value-diffs
  "extract only listings for which amountremaining has decreased"
  [old new-state]
  (let [new-values (->> new-state
                        (filter #(< (second %) (or (get old (first %)) 100000)))
                        (into {}))
        deltas (merge-with -
                           new-values
                           (select-keys old (keys new-values)))]
    [new-values deltas]))

(defn log-deltas
  [deltas]
  (doseq [d deltas]
    (log/infof
      "updating market state for %s: delta = %s" (first d) (second d))))

(defn update-state
  [new-listings]
  (let [new-amounts (->> new-listings
                         (map #(select-keys % [:ListingNumber :AmountRemaining]))
                         (reduce #(assoc %1 (:ListingNumber %2) (:AmountRemaining %2)) {}))]
    (swap! market-state
           (fn [s]
             (let [[diffs deltas] (value-diffs s new-amounts)]
               (when-not (empty? deltas)
                 (log-deltas deltas))
               (merge s diffs))))))

;(get @market-state 3838672)

(defn start-async-consumers
  [num-consumers future-ch]
  (dotimes [_ num-consumers]
    (as/thread
      (while true
        (let [item (query/parse-body @(as/<!! future-ch))]
          (let [updated (update-state item)]
            (jdbcd/with-connection *db*
              (storage/store-listings! *db* item))))))))

(defn query-and-store
  []
  (let [future-ch (as/chan 40)
        future-depth (atom 0)
        listing-ch (as/chan 500)
        listing-depth (atom 0)]
    (start-async-producer future-ch)
    (start-async-consumers 4 future-ch)))
