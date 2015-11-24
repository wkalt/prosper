(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [clojure.tools.logging :as log]
            [prosper.config :refer [*user* *pw* *client-id* *client-secret*]]
            [cheshire.core :as json]))

(def base-url "https://api.sandbox.prosper.com/v1/")

(def op-map {">=" "ge"
             "<=" "le"
             "<" "lt"
             ">" "gt"
             "=" "eg"
             "!=" "neq"})

(def access-token (atom nil))
(def refresh-token (atom nil))

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
        (json/parse-string true)
        :result)
    (log/errorf "HTTP request received %s. error is %s body is %s"
                status (:error response) (:body response))))

(defn request-access-token
  []
  (let [{:keys [access_token refresh_token]}
        (-> (http/post
              (format "%ssecurity/oauth/token?grant_type=password&client_id=%s&client_secret=%s&username=%s&password=%s"
                      base-url *client-id* *client-secret* *user* *pw*)
              {:content-type :json})
            :body
            (json/parse-string true))]
    (swap! access-token (constantly access_token))
    (swap! refresh-token (constantly refresh_token))))

(defn refresh-access-token
  []
  (let [{:keys [access_token refresh_token]}
        (-> (http/post
              (format "%ssecurity/oauth/token?grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s"
                      base-url *client-id* *client-secret* @refresh-token)
              {:content-type :json})
            :body
            (json/parse-string true))]
    (swap! access-token (constantly access_token))
    (swap! refresh-token (constantly refresh_token))))

(defn kit-wrap
  [query-string]
  (let [headers {"Authorization" (str "bearer " @access-token)}]
    (kit/get query-string {:accept :json :headers headers})))

(defn kit-get
  ([endpoint]
   (kit-wrap (str base-url endpoint)))
  ([endpoint query]
   (kit-wrap (format "%s%s?$filter=%s" base-url endpoint (compile-term query)))))
