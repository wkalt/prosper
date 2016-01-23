(ns prosper.service
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [prosper.collection :as collection]
            [prosper.migrate :as migrate]
            [environ.core :refer [env]]
            [prosper.query :refer [request-access-token refresh-access-token]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [overtone.at-at :as atat]))

(def nrepl-session (atom nil))

(defn start-prosper-service
  [db release-rate base-rate storage-threads]
  (future
    (collection/query-and-store db release-rate base-rate storage-threads)))

(defn attach-nrepl-server
  [{:keys [enabled port] :as nrepl-config}]
  (if (and nrepl-config (true? enabled))
    (swap! nrepl-session (fn [x] (start-server :port port)))))

(defn -main
  [foo]
  (let [cred-refresh-pool (atat/mk-pool)
        db (env :database)
        {:keys [client-id client-secret
                username password
                storage-threads release-rate base-rate]} (env :prosper)
        nrepl-config (env :nrepl)
        refresh-token #(refresh-access-token
                         client-id client-secret username password)]
    (log/info "Running migrations")
    (migrate/migrate! db)
    (request-access-token client-id client-secret username password)
    (atat/every (* 10 60 1000) refresh-token cred-refresh-pool)
    (start-prosper-service db release-rate base-rate storage-threads)
    (attach-nrepl-server nrepl-config)))
