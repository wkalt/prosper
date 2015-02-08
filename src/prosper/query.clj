(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [cheshire.core :as json]))

(def user "snoop_user@gmail.com")
(def pw "pass_for_snoops")
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
  [resp]
  (-> (:body resp)
      (json/parse-string true)))

(defn wrap-with-http
  [query-string]
  (http/get query-string {:accept :json :basic-auth [user pw]}))

(defn kit-wrap
  [query-string]
  (kit/get query-string {:accept :json :basic-auth [user pw]}))

(defn kit-get
  ([endpoint]
   (kit-wrap (str base-url endpoint)))
  ([endpoint query]
   (kit-wrap (format "%s%s?$filter=%s" base-url endpoint (compile-term query)))))

(defn query-get
  ([endpoint]
   (parse-body (wrap-with-http (str base-url endpoint))))
  ([endpoint query]
   (parse-body (wrap-with-http (format "%s%s?$filter=%s" base-url endpoint (compile-term query))))))

(defn query-string
  ([endpoint]
   (str base-url endpoint))
  ([endpoint query]
   (format "%s%s?$filter=%s" base-url endpoint (compile-term query))))
