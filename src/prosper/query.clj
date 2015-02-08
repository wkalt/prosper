(ns prosper.query
  (:require [clj-http.client :as http] 
            [cheshire.core :as json]))

(def user "snoop_user@gmail.com")
(def pw "pass_for_snoops")
(def base-url "https://api.prosper.com/api/")

(defmulti compile-term
  (fn [a &] (type a)))

(defn compile-eq
  [[op a b]]
  (let [sep (case op
              ">=" "ge"
              "<=" "le"
              "<" "lt"
              ">" "gt"
              "=" "eq"
              "!=" "neq")]
    (format "%s+%s+%s" a sep (compile-term b))))

(declare compile-and)

(defmethod compile-term clojure.lang.PersistentVector
  [[a b c]]
  (case a
    "and" (compile-and [a b c])
    (">=" "<=" "<" ">" "!=" "=") (compile-eq [a b c])))

(defmethod compile-term :default
  [x]
  (if (string? x)
    (format "'%s'" x)
    (str x)))

(defn compile-and
  [[_ a b]]
  (format "%s+and+%s" (compile-term a) (compile-term b)))


(defn wrap-with-http
  [query-string]
  (let [resp (http/get query-string {:accept :json :basic-auth [user pw]})]
    (json/parse-string (:body resp) true)))

(defn query-get
  ([endpoint]
   (wrap-with-http (str base-url endpoint)))
  ([endpoint query]
  (wrap-with-http (format "%s%s?$filter=%s" base-url endpoint (compile-term query)))))
