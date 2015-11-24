(ns prosper.config)

(def ^:dynamic *config* nil)
(def ^:dynamic *db* nil)
(def ^:dynamic *user* nil)
(def ^:dynamic *pw* nil)
(def ^:dynamic *base-rate* nil)
(def ^:dynamic *release-rate* nil)
(def ^:dynamic *storage-threads* nil)
(def ^:dynamic *client-id* nil)
(def ^:dynamic *client-secret* nil)

(defn load-config!
  [config]
  (let [{:keys [username password base-rate release-rate storage-threads
                client-secret client-id]}
        (:prosper config)]
    (alter-var-root #'*config* (constantly config))
    (alter-var-root #'*client-id* (constantly client-id))
    (alter-var-root #'*client-secret* (constantly client-secret))
    (alter-var-root #'*db* (constantly (:database config)))
    (alter-var-root #'*user* (constantly username))
    (alter-var-root #'*pw* (constantly password))
    (alter-var-root #'*base-rate* (constantly base-rate))
    (alter-var-root #'*storage-threads* (constantly storage-threads))
    (alter-var-root #'*release-rate* (constantly release-rate))))
