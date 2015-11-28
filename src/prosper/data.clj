(ns prosper.data
  (:require [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [prosper.config :refer [*db*]]))

(defn get-investments
  []
  (->> (jdbc/query *db* "select * from investments")
       (map #(update-in % [:bid_requests] json/parse-string true))))
