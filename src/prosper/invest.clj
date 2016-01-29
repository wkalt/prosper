(ns prosper.invest
  (:require [prosper.query :as query]
            [prosper.storage :as storage]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clj-time.core :refer [now]]
            [clojure.tools.logging :as log]))

(defn invest!
  "Invest bid-requests. Format should be [{:bid_amount amount :listing_id id}]"
  [bid-requests base-url db]
  (let [total (reduce + (map :bid_amount bid-requests))]
    (if (= (:actually-invest (env :prosper)) true)
      (try
        (let [{:keys [order_id order_date
                      bid_requests] :as body} @(->> {:bid_requests bid-requests}
                                                    (query/http-post "orders/" base-url)
                                                    query/parse-post-body)]
          (log/infof "Submitted order %s for %s at %s" order_id total order_date)
          (storage/record-investment! body db))
        (catch Exception e
          (log/errorf "Caught exception while posting investment! Exception: %s" e)))
      (log/infof "FAKE Submitted order for %s at %s" total (now)))))
