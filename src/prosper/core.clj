(ns prosper.core
  (:require [prosper.collection :as collection]))

(def ns-prefix "prosper")

(defn parse-collection
  [args]
  (case (first args)
    "-d" (collection/query-and-store (read-string (second args)) 333)))

(defn -main
  [& args]
  (case (first args)
    "collection" (parse-collection (rest args)))
  (System/exit 0))
