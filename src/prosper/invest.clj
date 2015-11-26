(ns prosper.invest
  (:require [prosper.query :as query]
            [clojure.tools.logging :as log]))

(defn invest!
  [amount listing_number]
  (let [payload {:bid_requests [{"bid_amount" amount "listing_id" listing_number}]}
        resp (->> payload
                  (query/http-post "orders/")
                  query/parse-post-body)
        {:keys [order_id order_date bid_requests]} resp
        total (reduce + (map :bid_amount bid_requests))]
    (log/infof "Submitted order %s for %s at %s" order_id total order_date)
    resp))
