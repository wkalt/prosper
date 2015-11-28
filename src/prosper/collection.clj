(ns prosper.collection
  (:require [prosper.query :as query]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now plus interval today-at minutes within?]]
            [clj-time.predicates :refer [weekend?]]
            [clj-time.local :refer [local-now]]
            [prosper.storage :as storage]
            [prosper.config :refer [*db* *release-rate* *base-rate*
                                    *storage-threads*]]
            [clojure.core.async :as as]))

(def market-state (atom {}))

(defn release-times
  []
  ;; TODO this is hardcoded
  (if (weekend? (local-now))
    [(interval (today-at 19 50) (today-at 20 15))]
    [(interval (today-at 16 50) (today-at 17 15))
     (interval (today-at 23 50) ;; this one spans midnight UTC
               (plus (today-at 23 50) (minutes 25)))]))

(defn in-release?
  []
  (boolean (some true? (map #(within? % (now)) (release-times)))))

(defn start-producer
  [future-ch]
  (as/thread
    (while true
      (Thread/sleep (if (in-release?) *release-rate* *base-rate*))
      (as/>!! future-ch (query/kit-get "search/listings")))))

(defn log-deltas
  [deltas]
  (doseq [[listing {:keys [prosper_rating lender_yield amount_remaining]}] deltas]
    (log/infof "updating market state: %s: (%s / %s / %s)"
               listing prosper_rating lender_yield amount_remaining)))

(defn coalesce-state
  [acc listing]
  (assoc acc (:listing_number listing)
    (select-keys listing [:amount_remaining :lender_yield :prosper_rating])))

(defn diff-amounts
  [a b]
  (assoc a :amount_remaining (- (:amount_remaining a) (:amount_remaining b))))

(defn <-or-nil?
  "returns true of a < b or b == nil"
  [a b]
  (if-let [amt (:amount_remaining b)]
    (< (:amount_remaining a) amt)
    true))

(defn value-diffs
  "extract only listings for which amountremaining has decreased"
  [old new-state]
  (let [new-values (->> new-state
                        (filter #(<-or-nil? (second %) (get old (first %))))
                        (into {}))
        deltas (merge-with diff-amounts
                           new-values (select-keys old (keys new-values)))]
    [new-values deltas]))

(defn update-state
  [new-listings market-state]
  (let [s' (reduce coalesce-state {} new-listings)]
    (swap! market-state
           (fn [s] (let [[diffs deltas] (value-diffs s s')]
                     (when-not (empty? deltas)
                       (log-deltas deltas))
                     (merge (select-keys s (keys s')) diffs))))))

(defn start-consumers
  [num-consumers future-ch market-state]
  (dotimes [_ num-consumers]
    (as/thread (while true
                 (let [item (query/parse-body @(as/<!! future-ch))]
                   (update-state item market-state)
                   (storage/store-listings! *db* item))))))

(defn query-and-store
  []
  (let [future-ch (as/chan 40)]
    (start-producer future-ch)
    (start-consumers *storage-threads* future-ch market-state)))
