(ns prosper.misc
  (:require [clj-http.client :as http]))

(defn select-historical
  [start end]
  (let [query-string (str "ListingsHistorical?$filter=LoanOriginationDate+ge+datetime'"
                          start "'+and+LoanOriginationDate+lt+datetime'" end "'")]
    (query-api query-string "snoop_user@gmail.com" "pass_for_snoops")))

(defn months-since
  [year month]
  (let [start (format "%s-%02d" year month)]
    (cond
      (= [year month] [2014 10]) [start]
      (= 12 month) (cons start (months-since (inc year) 1))
    :else (cons start (months-since year (inc month))))))

(defn download-prosper-history
  [outputdir year month]
  (let [months (months-since year month)]
    (loop [m months]
      (let [m1 (str (first m) "-01")
            m2 (str (second m) "-01")]
      (->> (select-historical m1 m2)
          :body
          (spit (str outputdir (format "/ListingsHistorical_%s_%s.json" m1 m2))))
      (println "completed " m1)
      (when (> (count m) 1)
        (recur (rest m)))))))
