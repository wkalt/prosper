(ns prosper.collection
  (:require [prosper.query :as query]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
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
                        ;; next line is dumb
                        (filter #(< (second %) (or (get old (first %)) 100000)))
                        (into {}))
        deltas (merge-with - new-values (select-keys old (keys new-values)))]
    [new-values deltas]))

(defn log-deltas
  [deltas]
  (->> deltas
       (map #(format "%s: %s, " (first %) (second %)))
       (string/join #" ")
       (log/infof "updating market state: %s")))

(defn update-state
  [new-listings]
  (let [s' (->> new-listings
                (map #(select-keys % [:ListingNumber :AmountRemaining]))
                (reduce #(assoc %1 (:ListingNumber %2) (:AmountRemaining %2)) {}))]
    (swap! market-state
           (fn [s]
             (let [[diffs deltas] (value-diffs s s')]
               (when-not (empty? deltas)
                 (log-deltas deltas))
               (merge s diffs))))))

(defn start-async-consumers
  [num-consumers future-ch]
  (dotimes [_ num-consumers]
    (as/thread (while true
                 (let [item (query/parse-body @(as/<!! future-ch))]
                   (jdbcd/with-connection *db*
                     (storage/store-listings! *db* item)))))))

(defn query-and-store
  []
  (let [future-ch (as/chan 40)]
    (start-async-producer future-ch)
    (start-async-consumers *storage-threads* future-ch)))
