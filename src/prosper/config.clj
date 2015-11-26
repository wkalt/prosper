(ns prosper.config
  (:require [environ.core :refer [env]]))

(def ^:dynamic *config* nil)
(def ^:dynamic *db* nil)
(def ^:dynamic *base-rate* nil)
(def ^:dynamic *release-rate* nil)
(def ^:dynamic *storage-threads* nil)

(defn load-config!
  [config]
  (let [{:keys [database base-rate release-rate storage-threads]}
        (:prosper config)]
    (alter-var-root #'*config* (constantly config))
    (alter-var-root #'*db* (constantly (:database config)))
    (alter-var-root #'*base-rate* (constantly base-rate))
    (alter-var-root #'*storage-threads* (constantly storage-threads))
    (alter-var-root #'*release-rate* (constantly release-rate))))
