(ns prosper.service
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [prosper.collection :as collection]
            [prosper.migrate :as migrate]
            [environ.core :refer [env]]
            [prosper.query :refer [request-access-token refresh-access-token]]
            [puppetlabs.trapperkeeper.core :refer [defservice]]
            [overtone.at-at :as atat]))

(defn start-prosper-service
  [context db release-rate base-rate storage-threads]
  (future (collection/query-and-store db release-rate base-rate
                                      storage-threads))
  context)

(defservice prosper-service
  []
  (start [this context]
         (let [cred-refresh-pool (atat/mk-pool)
               db (env :database)
               {:keys [client-id client-secret
                       username password
                       storage-threads release-rate base-rate]} (env :prosper)
               refresh-token #(refresh-access-token
                                client-id client-secret username password)]
           (log/info "Running migrations")
           (migrate/migrate! db)
           (request-access-token client-id client-secret username password)
           (atat/every (* 10 60 1000) refresh-token cred-refresh-pool)
           (start-prosper-service context db release-rate base-rate
                                  storage-threads))))
