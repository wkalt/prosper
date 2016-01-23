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
