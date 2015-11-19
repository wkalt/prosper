(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [clojure.tools.logging :as log]
            [prosper.config :refer [*user* *pw*]]
            [cheshire.core :as json]))

(def base-url "https://api.prosper.com/api/")

(def op-map {">=" "ge"
             "<=" "le"
             "<" "lt"
             ">" "gt"
             "=" "eg"
             "!=" "neq"})

(declare compile-and)
(declare compile-eq)

(defmulti compile-term
  (fn [a &] (type a)))

(defmethod compile-term clojure.lang.PersistentVector
  [[a b c]]
  (case a
    "and" (compile-and [a b c])
    (">=" "<=" "<" ">" "!=" "=") (compile-eq [a b c])))

(defmethod compile-term :default
  [x]
  (if (string? x) (format "'%s'" x) (str x)))

(defn compile-eq
  [[op a b]]
  (format "%s+%s+%s" a (get op-map op) (compile-term b)))

(defn compile-and
  [[_ a b]]
  (format "%s+and+%s" (compile-term a) (compile-term b)))

(defn parse-body
  [{:keys [status body message] :as response}]
  (if (= 200 status)
    (-> body
        (json/parse-string true))
    (log/errorf "HTTP request received %s. error is %s body is %s"
                status (:error response) (:body response))))

(defn kit-wrap
  [query-string]
  (kit/get query-string {:accept :json :basic-auth [*user* *pw*]}))

(defn kit-get
  ([endpoint]
   (kit-wrap (str base-url endpoint)))
  ([endpoint query]
   (kit-wrap (format "%s%s?$filter=%s" base-url endpoint (compile-term query)))))
