(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [clojure.tools.logging :as log]
            [prosper.config :refer [*user* *pw* *client-id* *client-secret*]]
            [cheshire.core :as json]))

(def base-url "https://api.sandbox.prosper.com/v1/")

(def access-token (atom nil))
(def refresh-token (atom nil))

(defn parse-body
  [{:keys [status body error]}]
  (if (= 200 status)
    (:result (json/parse-string body true))
    (log/errorf
      "HTTP request received %s. error is %s body is %s" status error body)))

(defn request-access-token
  []
  (let [params {:grant_type "password"
                :client_id *client-id*
                :client_secret *client-secret*
                :username *user*
                :password *pw*}
        resp (-> (str base-url "security/oauth/token")
                 (http/post {:form-params params})
                 :body
                 (json/parse-string true))]
    (swap! access-token (constantly (:access_token resp)))
    (swap! refresh-token (constantly (:refresh_token resp)))))

(defn refresh-access-token
  []
  (let [params {:grant_type "refresh_token"
                :client_id *client-id*
                :client_secret *client-secret*
                :refresh_token @refresh-token}
        resp (-> (str base-url "security/oauth/token")
                 (http/post {:headers {"Content-Type" "application/x-www-form-urlencoded"}
                             :form-params params :oauth-token @access-token})
                 :body
                 (json/parse-string true))]
    (log/info "Refreshing access token")
    (swap! access-token (constantly (:access_token resp)))
    (swap! refresh-token (constantly (:refresh_token resp)))))

(defn kit-get
  ([endpoint]
   (kit/get (str base-url endpoint) {:accept :json :oauth-token @access-token})))

(defn http-post
  ([endpoint params]
   (http/post (str base-url endpoint)
              {:oauth-token @access-token :content-type :json
               :form-params params})))

(defn parse-post-body
  [{:keys [status body error]}]
  (if (= 200 status)
    (json/parse-string body true)
    (log/errorf
      "HTTP request received %s. error is %s body is %s" status error body)))
