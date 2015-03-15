(ns prosper.config
  (:require [clojure-ini.core :as ini]))

(def ^:dynamic *config* nil)
(def ^:dynamic *db* nil)
(def ^:dynamic *user* nil)
(def ^:dynamic *pw* nil)
(def ^:dynamic *base-rate* nil)
(def ^:dynamic *release-rate* nil)

(defn load-config
  [path]
  (let [config-map (ini/read-ini path :keywordize? true)
        {:keys [username password base-rate release-rate]} (:prosper config-map)]
    (alter-var-root #'*config* (constantly config-map))
    (alter-var-root #'*db* (constantly (:database config-map)))
    (alter-var-root #'*user* (constantly username))
    (alter-var-root #'*pw* (constantly password))
    (alter-var-root #'*base-rate* (constantly (read-string base-rate)))
    (alter-var-root #'*release-rate* (constantly (read-string release-rate)))
    config-map))
