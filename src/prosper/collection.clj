(ns prosper.collection
  (:require [prosper.query :as query]
            [cemerick.url :refer [url-encode]]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now before?]]
            [prosper.storage :refer [store-listings! release-end-time]]
            [clojure.core.async :as as]))

(defn string-max
  ([a] a)
  ([a b] (if (neg? (compare a b)) b a))
  ([a b & more] (reduce string-max (string-max a b) more)))

;; currently the v1 API does not provide a way to get the most recently updated
;; listings, so we're stuck with pulling them all. This can be refined if
;; support is ever added.

(defn cached-count
  [endpoint market-state base-url]
  (let [market-count (count @market-state)]
    (if (zero? market-count)
      (query/get-count endpoint base-url)
      market-count)))

(defn fetch-listings
  [endpoint base-url market-state]
  (let [cached-count (cached-count endpoint market-state base-url)]
    (for [offset (range 0 cached-count 50)]
      (query/kit-get
        (str endpoint "?sort_by=listing_number%20desc%20&limit=50&offset=" offset)
        base-url))))

(defn get-listings
  [listing-future]
  (map :listing_number (query/parse-body @listing-future)))

(defn prune-market-state!
  [market-state endpoint base-url]
  (let [available-listings (->> (fetch-listings endpoint base-url market-state)
                                (map get-listings)
                                (apply concat))
        state-count (count @market-state)]
    (swap! market-state #(select-keys % available-listings))
    (log/infof "Removed %s listings from market state."
               (- state-count (count @market-state)))))

(defn in-release?
  []
  (before? (now) @release-end-time))

(defn start-producer
  [future-ch release-rate base-rate base-url market-state]
  (as/thread
    (let [listings-endpoint "search/listings?include_creditbureau_values=true"]
      (while true
        (let [sleep-duration (if (in-release?) release-rate (/ base-rate 7))]
          (doseq [listing-future (fetch-listings listings-endpoint base-url market-state)]
            (as/>!! future-ch listing-future)
            (Thread/sleep sleep-duration)))))))

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
  [db release-rate base-rate storage-threads base-url market-state]
  (let [future-ch (as/chan 40)]
    (start-producer future-ch release-rate base-rate base-url market-state)
    (start-consumers storage-threads future-ch market-state db)))
