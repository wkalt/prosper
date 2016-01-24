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
  [db release-rate base-rate storage-threads base-url market-state]
  (future
    (collection/query-and-store db release-rate base-rate storage-threads base-url
                                market-state)))

(defn attach-nrepl-server
  [{:keys [enabled port] :as nrepl-config}]
  (if (and nrepl-config (true? enabled))
    (swap! nrepl-session (fn [x] (start-server :port port)))))

(defn -main
  []
  (let [job-pool (atat/mk-pool)
        db (env :database)
        {:keys [client-id client-secret
                username password
                storage-threads release-rate base-rate base-url]} (env :prosper)
        nrepl-config (env :nrepl)
        market-state (atom {})
        refresh-token #(refresh-access-token
                         client-id client-secret username password base-url)
        prune-market #(collection/prune-market-state! market-state "search/listings" base-url)
        token-refresh-interval (* 10 60 1000)
        market-prune-interval (* 1000 60 30)]
    (log/info "Running migrations")
    (migrate/migrate! db)
    (request-access-token client-id client-secret username password base-url)
    (atat/every token-refresh-interval refresh-token job-pool
                :initial-delay token-refresh-interval)
    (atat/every market-prune-interval prune-market job-pool
                :initial-delay market-prune-interval)
    (start-prosper-service db release-rate base-rate storage-threads base-url
                           market-state)
    (attach-nrepl-server nrepl-config)))
