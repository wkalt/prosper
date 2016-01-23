(ns prosper.config
  (:require [environ.core :refer [env]]
            [schema.core :as s]))

(def global-schema
  {(s/optional-key :logging-config) s/Str})

(def prosper-schema
  {:username s/Str
   :password s/Str
   :client-id s/Str
   :client-secret s/Str
   :release-rate s/Int
   :base-rate s/Int
   :storage-threads s/Int
   :weekday-release [s/Str]
   :weekend-release [s/Str]})

(def webserver-schema
  {:host s/Str
   :port s/Int})

(def database-schema
  {:subprotocol s/Str
   :subname s/Str
   :user s/Str
   :password s/Str
   :classname s/Str})

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
