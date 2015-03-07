(ns prosper.collection
  (:require [prosper.query :as query]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now plus secs after?]]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [prosper.storage :as storage]
            [clojure.java.jdbc :as jdbc]
            [clojure.core.async :as as]))

(def postgres-db {:subprotocol "postgresql"
                  :subname "//localhost:5432/prosper"
                  :user "prosper"
                  :password "prosper"
                  :classname "org.postgresql.Driver"})

(defn query-and-store
  [duration interval]
  (let [future-ch (as/chan 40)
        future-depth (atom 0)
        listing-ch (as/chan 500)
        listing-depth (atom 0)
        end-time (plus (now) (secs duration))]

    (log/info
      (format "Starting query run of %s seconds at interval %s ms."
              duration interval))

    (as/thread
      (pmap
        (fn [x]
          (jdbcd/with-connection postgres-db
            (while true
              (storage/store-listings! (as/<!! listing-ch))
              (swap! listing-depth dec))))
        (range 8)))

    (as/thread
      (while true
        (when (pos? @future-depth)
          (as/>!! listing-ch (query/parse-body @(as/<!! future-ch)))
          (swap! future-depth dec)
          (swap! listing-depth inc))))

    (while (after? end-time (now))
      (Thread/sleep interval)
      (as/>!! future-ch (query/kit-get "Listings"))
      (swap! future-depth inc))

    (while (not (zero? @listing-depth))
      (Thread/sleep 500)) ;; sleep half secs until listings drain

    (as/close! future-ch)
    (as/close! listing-ch)))
