(ns prosper.core
  (:require [prosper.collection :as collection]
            [prosper.migrate :as migrate]))

(def ns-prefix "prosper")

(defn parse-collection
  [args]
  (case (first args)
    "-d" (collection/query-and-store (read-string (second args)) 333)))

(defn -main
  [& args]
  (case (first args)
    "collection" (parse-collection (rest args))
    "migrate" (migrate/migrate!))
  (System/exit 0))
