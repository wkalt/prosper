(ns prosper.collection
  (:require [prosper.query :as query]
            [clj-time.core :refer [now plus secs after?]]
            [clj-http.client :as http]
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
  (let [future-ch (as/chan 40) ;; thread constrained
        future-depth (atom 0)
        listing-depth (atom 0)
        listing-ch (as/chan 500)
        end-time (plus (now) (secs duration))
        a1 (atom 0)
        a2 (atom 0)]

    (println "running")

    (as/thread
      (pmap
        (fn [x]

          (jdbcd/with-connection postgres-db
            (while true
              (let [listings (as/<!! listing-ch)]
                (swap! a2 inc)
                (println "listing-depth" listing-depth)
                (storage/store-listings! listings)
                (swap! listing-depth dec))))) (range 8)))

    (as/thread
      (while true
        (when (pos? @future-depth)
          (let [v (as/<!! future-ch)]
            (swap! a1 inc)
            (swap! future-depth dec)
            (swap! listing-depth inc)
            (as/>!! listing-ch (query/parse-body @v))))))

    (while (after? end-time (now))
      (Thread/sleep interval)
      (swap! future-depth inc)
      (as/>!! future-ch (query/kit-get "Listings")))

    (while (not (zero? @listing-depth))
      (Thread/sleep 500)) ;; sleep half secs until listings drain
    (as/close! future-ch)
    (println "processed" a1 "messages and stored" a2)))

(defn store-listings-historical
  []
  (let [listings (query/query-get "ListingsHistorical")]
    (storage/store-listings! listings)))

(def cli-options
  [["-d" "--duration DURATION" "Number of seconds over which to query and store"]
   ["-h" "--historical" "download and store all historical listings"]])

#_ (query-and-store 100)
#_ (store-listings-historical)
