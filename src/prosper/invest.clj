(ns prosper.invest
  (:require [prosper.query :as query]
            [prosper.storage :as storage]
            [prosper.config :refer [*db*]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clojure.tools.logging :as log]))

(defn invest!
  "invest bid-requests. Format should be [{:bid_amount amount :listing_id id}]"
  [bid-requests]
  (let [payload {:bid_requests bid-requests}
        resp (->> {:bid_requests bid-requests}
                  (query/http-post "orders/")
                  query/parse-post-body)
        {:keys [order_id order_date bid_requests]} resp
        total (reduce + (map :bid_amount bid_requests))]
    (log/infof "Submitted order %s for %s at %s" order_id total order_date)
    (jdbcd/with-connection *db*
      (storage/store-investment! resp *db*))))
