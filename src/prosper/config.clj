(ns prosper.config
  (:require [clojure-ini.core :as ini]))

(def ^:dynamic *config* nil)
(def ^:dynamic *db* nil)
(def ^:dynamic *user* nil)
(def ^:dynamic *pw* nil)

(defn load-config
  [path]
  (let [config-map (ini/read-ini path :keywordize? true)]
    (alter-var-root #'*config* (constantly config-map))
    (alter-var-root #'*db* (constantly (:database config-map)))
    (alter-var-root #'*user* (constantly (get-in config-map [:prosper :username])))
    (alter-var-root #'*pw* (constantly (get-in config-map [:prosper :password])))
    config-map))
