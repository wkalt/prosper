(ns prosper.collection
  (:require [prosper.query :as query]
            [cemerick.url :refer [url-encode]]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now before?]]
            [environ.core :refer [env]]
            [prosper.storage :refer [store-listings! release-end-time]]
            [clojure.core.async :as as]))

(def market-state (atom {}))
; (def last-updated-date (atom nil))

(defn string-max
  ([a] a)
  ([a b] (if (neg? (compare a b)) b a))
  ([a b & more] (reduce string-max (string-max a b) more)))

;; TODO: use this if prosper ever makes last_updated_date queryable
;; (defn fetch-listings
;;   [endpoint]
;;   (if @last-updated-date
;;     [(query/kit-get (str endpoint "?last_updated_date_min=" (url-encode @last-updated-date)))]
;;     (let [total-count (query/get-count "search/listings?offset=10000")]
;;       (for [offset (range 0 total-count 50)]
;;         (do (Thread/sleep 100)
;;             (query/kit-get (str endpoint "?sort_by=listing_number&limit=50&offset=" offset)))))))

(defn fetch-listings
  [endpoint base-url]
  (let [total-count (query/get-count "search/listings?offset=10000" base-url)]
    (for [offset (range 0 total-count 50)]
      (query/kit-get (str endpoint "?sort_by=listing_number&limit=50&offset=" offset)
                     base-url))))

(defn in-release?
  []
  (before? (now) @release-end-time))

(defn start-producer
  [future-ch release-rate base-rate base-url]
  (as/thread
    (let [listings-endpoint "search/listings?include_creditbureau_values=true"]
      (while true
        (Thread/sleep (if (in-release?) release-rate base-rate))
        (doseq [listing-future (fetch-listings listings-endpoint base-url)]
          (as/>!! future-ch listing-future))))))

(defn log-deltas
  [deltas]
  (doseq [[listing {:keys [prosper_rating lender_yield amount_remaining]}] deltas]
    (when-not (zero? amount_remaining)
      (log/infof "updating market state: %s: (%s / %s / %s)"
                 listing prosper_rating lender_yield amount_remaining))))

(defn coalesce-state
  [acc listing]
  (assoc acc (:listing_number listing)
    (select-keys listing [:amount_remaining :lender_yield :prosper_rating])))

(defn take-lower-amt
  [a b]
  (if (< (:amount_remaining b) (:amount_remaining a)) b a))

(defn value-diffs
  "extract only listings for which amount_remaining has decreased"
  [old-state new-state]
  (let [new-values (merge-with take-lower-amt new-state old-state)
        deltas (merge-with
                 #(update %1 :amount_remaining - (:amount_remaining %2))
                 new-values (select-keys old-state (keys new-values)))]
    [new-values deltas]))

(defn update-state-fn
  [s']
  (fn [s]
    (let [[diffs deltas] (value-diffs s s')]
      (when (seq deltas)
        (log-deltas deltas))
      (merge s diffs))))

(defn update-state!
  [new-listings market-state]
;  (swap! last-updated-date
;         (fn [x] (apply string-max (cons x (map :last_updated_date new-listings)))))
  (let [s' (reduce coalesce-state {} new-listings)]
    (swap! market-state (update-state-fn s'))))

(defn start-consumers
  [num-consumers future-ch market-state db]
  (dotimes [_ num-consumers]
    (as/thread (while true
                 (let [item (query/parse-body @(as/<!! future-ch))]
                   (update-state! item market-state)
                   (store-listings! db item))))))

(defn query-and-store
  [db release-rate base-rate storage-threads base-url]
  (let [future-ch (as/chan 40)]
    (start-producer future-ch release-rate base-rate base-url)
    (start-consumers storage-threads future-ch market-state db)))
