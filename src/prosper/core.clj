(ns prosper.core
  (:gen-class)
  (:require [prosper.collection :as collection]))

(defn -main
  [& args]
  (case (first args)
    "collection" (collection/query-and-store))
  (System/exit 0))
