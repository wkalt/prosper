(ns prosper.data
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [environ.core :as env]))

(defn get-investments
  []
  (let [db (env :database)]
    (->> (jdbc/query db "select * from investments")
         (map #(update % :bid_requests json/parse-string true))
         (sort-by :order_date))))
