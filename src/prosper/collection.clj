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

(defn log-delta
  [delta]
  (let [listingnumber (first delta)
        {:keys [ProsperRating LenderYield AmountRemaining]} (second delta)]
    (log/infof "updating market state: %s: (%s / %s / %s), " listingnumber ProsperRating LenderYield AmountRemaining)))

(defn log-deltas
  [deltas]
  (doseq [d deltas]
    (log-delta d)))

(defn weak-
  [a b]
  (try
    (- a b)
    (catch Exception _
      a)))

(defn coalesce-state
  [acc listing]
  (assoc acc (:ListingNumber listing)
    (select-keys listing [:AmountRemaining :LenderYield :ProsperRating])))

(defn listings->market-state
  [listings]
  (->> listings
       (reduce coalesce-state {})))

(defn value-diffs
  "extract only listings for which amountremaining has decreased"
  [old new-state]
  (let [new-values (->> new-state
                        ;; next line is dumb
                        (filter #(< (:AmountRemaining (second %)) (or (:AmountRemaining (get old (first %))) 100000)))
                        (into {}))
        deltas (merge-with (partial merge-with weak-) new-values (select-keys old (keys new-values)))]
    [new-values deltas]))

(defn update-state
  [new-listings]
  (let [s' (listings->market-state new-listings)]
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
                     (update-state item)
                     (storage/store-listings! *db* item)))))))

(defn query-and-store
  []
  (let [future-ch (as/chan 40)]
    (start-async-producer future-ch)
    (start-async-consumers *storage-threads* future-ch)))
