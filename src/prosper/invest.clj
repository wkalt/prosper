(ns prosper.invest
  (:require [prosper.query :as query]
            [prosper.storage :as storage]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [prosper.data :refer [get-investments]]
            [clojure.tools.logging :as log]))

(defn invest!
  "invest bid-requests. Format should be [{:bid_amount amount :listing_id id}]"
  [bid-requests base-url]
  (let [db (env :database)]
    (let [payload {:bid_requests bid-requests}
          resp (->> {:bid_requests bid-requests}
                    (query/http-post "orders/" base-url)
                    query/parse-post-body)
          {:keys [order_id order_date bid_requests]} resp
          total (reduce + (map :bid_amount bid_requests))]
      (log/infof "Submitted order %s for %s at %s" order_id total order_date)
      (storage/store-investment! resp db))))
