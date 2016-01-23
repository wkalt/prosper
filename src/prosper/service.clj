(ns prosper.service
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [prosper.prosper-web-core :as core]
            [prosper.collection :as collection]
            [prosper.migrate :as migrate]
            [prosper.config :refer [load-config!]]
            [prosper.query :refer [request-access-token refresh-access-token]]
            [puppetlabs.trapperkeeper.core :refer [defservice]]
            [overtone.at-at :as atat]
            [puppetlabs.trapperkeeper.services :as tk-services]))

(defn start-prosper-service
  [context config service get-route]
  (let [host (get-in config [:webserver :host])
        port (get-in config [:webserver :port])
        url-prefix (get-route service)]
    (log/infof "Hello web service started; visit http://%s:%s%s/world
                to check it out!" host port url-prefix)
    (future (collection/query-and-store))
    context))

(defservice hello-web-service
  [[:ConfigService get-config]
   [:WebroutingService add-ring-handler get-route]]
  (start [this context]
         (log/info "Initializing hello webservice")
         (let [url-prefix (get-route this)
               config (get-config)
               cred-refresh-pool (atat/mk-pool)]
           (load-config! config)
           (log/info "Running migrations")
           (migrate/migrate!)
           (add-ring-handler this
                             (compojure/context
                               url-prefix []
                               (core/app (tk-services/get-service
                                           this :HelloService))))
           (assoc context :url-prefix url-prefix)
           (request-access-token)
           (atat/every (* 10 60 1000) refresh-access-token cred-refresh-pool)
           (start-prosper-service context config this get-route))))
