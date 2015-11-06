(ns prosper.collection
  (:require [prosper.query :as query]
            [clojure.tools.logging :as log]
            [clj-time.core :refer [now plus secs after? date-time] :as t]
            [clj-time.predicates :as pr]
            [clj-time.local :as l]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [prosper.storage :as storage]
            [clojure.java.jdbc :as jdbc]
            [prosper.config :refer [*db* *release-rate* *base-rate*]]
            [clojure.core.async :as as]))

(defn release-times
  []
  (if (pr/weekend? (l/local-now))
    [(t/interval (t/today-at 19 50) (t/today-at 20 15))]
    [(t/interval (t/today-at 15 50) (t/today-at 16 15))
     (t/interval (t/today-at 23 50) ;; this one spans midnight UTC
                 (plus (t/today-at 23 50) (t/minutes 25)))]))

(defn in-release?
  []
  (boolean (some true? (map #(t/within? % (now)) (release-times)))))

(defn create-listing-consumer-watch
  [listing-ch]
  (fn [key atom old-state new-state]
    (when (< old-state new-state)
      (jdbcd/with-connection *db*
        (storage/store-listings! *db* (as/<!! listing-ch)))
      (swap! atom dec))))

(defn create-future-consumer-watch
  [future-ch listing-depth listing-ch]
  (fn [key atom old-state new-state]
    (when (< old-state new-state)
      (as/>!! listing-ch (query/parse-body @(as/<!! future-ch)))
      (swap! atom dec)
      (swap! listing-depth inc))))

(defn query-and-store
  []
  (let [future-ch (as/chan 40)
        future-depth (atom 0)
        listing-ch (as/chan 500)
        listing-depth (atom 0)]

    (add-watch future-depth :future-consumer
               (create-future-consumer-watch future-ch listing-depth
                                             listing-ch))
    (log/info "registered future consumer")
    (add-watch listing-depth :listing-consumer
               (create-listing-consumer-watch listing-ch))
    (log/info "registered listing consumer")

    (while true
      (Thread/sleep (if (in-release?) *release-rate* *base-rate*))
      (let [result (query/kit-get "Listings")]
        (as/>!! future-ch result))
      (swap! future-depth inc))))
