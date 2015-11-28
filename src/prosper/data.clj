(ns prosper.data
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [prosper.config :refer [*db*]]))

(defn get-investments
  []
  (->> (jdbc/query *db* "select * from investments")
       (map #(update % :bid_requests json/parse-string true))
       (sort-by :order_date)))
