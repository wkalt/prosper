(ns prosper.service
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [prosper.prosper-web-core :as core]
            [prosper.collection :as collection]
            [prosper.migrate :as migrate]
            [prosper.config :refer [load-config!]]
            [puppetlabs.trapperkeeper.core :as trapperkeeper]
            [puppetlabs.trapperkeeper.services :as tk-services]))

(defn start-prosper-service
  [context config service get-in-config get-route]
  (let [host (get-in-config [:webserver :host])
        port (get-in-config [:webserver :port])
        url-prefix (get-route service)]
    (log/infof "Hello web service started; visit http://%s:%s%s/world to check it out!"
               host port url-prefix)
    (future (collection/query-and-store))
    context))

(trapperkeeper/defservice hello-web-service
  [[:ConfigService get-in-config get-config]
   [:WebroutingService add-ring-handler get-route]
   HelloService]
  (init [this context]
        (log/info "Initializing hello webservice")
        (let [url-prefix (get-route this)
              config (get-config)]
          (load-config! config)
          (log/info "Running migrations")
          (migrate/migrate!)
          (add-ring-handler this
                            (compojure/context
                              url-prefix []
                              (core/app (tk-services/get-service this :HelloService))))
          (assoc context :url-prefix url-prefix)))

  (start [this context]
         (start-prosper-service context (get-config) this get-in-config get-route)))

(defn -main
  [& args]
  (apply trapperkeeper/main args))
