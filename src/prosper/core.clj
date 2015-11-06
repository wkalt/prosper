(ns prosper.core
  (:gen-class)
  (:require [prosper.collection :as collection]
            [clojure.tools.logging :as log]
            [prosper.config :as config]
            [prosper.migrate :as migrate]))

(defn validate-cli-args
  [[command & args]]
  (let [cli-map (reduce conj {} (map vec (partition 2 args)))]
    (cond
      (not (get cli-map "-c"))
      (log/error "-c (config) is required")

      :else
      cli-map)))

(defn -main
  [& args]
  (let [cli-map (validate-cli-args args)
        config-map (config/load-config (get cli-map "-c"))]
    (case (first args)
      "collection" (collection/query-and-store)
      "migrate" (migrate/migrate!))
    (System/exit 0)))
