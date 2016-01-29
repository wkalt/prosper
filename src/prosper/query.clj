(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [clojure.tools.logging :as log]
            [throttler.core :refer [throttle-fn]]
            [cheshire.core :as json]))

(def base-url "https://api.prosper.com/v1/")

(def access-token (atom nil))
(def refresh-token (atom nil))

(defn parse-body
  [{:keys [status body] :as resp}]
  (if (= 200 status)
    (let [body' (json/parse-string body true)]
      (or (:result body') body'))
    (log/errorf "HTTP request received %s. Response is %s" status resp)))

(defn update-tokens!
  [{:keys [access_token refresh_token]}]
  (swap! access-token (constantly access_token))
  (swap! refresh-token (constantly refresh_token)))

(defn request-access-token
  [client-id client-secret username password base-url]
  (try
    (let [params {:grant_type "password"
                  :client_id client-id
                  :client_secret client-secret
                  :username username
                  :password password}
          resp (-> (str base-url "security/oauth/token")
                   (http/post {:form-params params})
                   parse-body)]
      (update-tokens! resp)
      (log/infof "Received new access token: expires in %s" (:expires_in resp)))
    (catch Exception e
      (log/error
        (format "Caught exception while requesting access token: %s" e)))))

(defn refresh-access-token
  [client-id client-secret username password base-url]
  (try
    (let [params {:grant_type "refresh_token"
                  :client_id client-id
                  :client_secret client-secret
                  :refresh_token @refresh-token}
          resp (-> (str base-url "security/oauth/token")
                   (http/post {:form-params params :oauth-token @access-token})
                   parse-body)]
      (update-tokens! resp)
      (log/infof "Refreshed access token. Expires in %s" (:expires_in resp)))
    (catch Exception e
      (log/error
        "Caught exception while refreshing access token. Requesting new token.")
      (request-access-token client-id client-secret username password base-url))))

(defn kit-get*
  [endpoint base-url]
  (kit/get (str base-url endpoint) {:accept :json :oauth-token @access-token}))

(def kit-get (throttle-fn kit-get* 16 :second))

(defn http-post
  ([endpoint base-url params]
   (kit/post (str base-url endpoint)
             {:oauth-token @access-token :content-type :json
              :form-params params})))

(defn parse-post-body
  [{:keys [status body error]}]
  (if (= 200 status)
    (json/parse-string body true)
    (log/errorf
      "HTTP request received %s. Body is %s" status body)))

(defn get-count
  [endpoint base-url]
  (-> (http/get (str base-url endpoint) {:accept :json :oauth-token @access-token})
      :body
      (json/parse-string true)
      :total_count))
