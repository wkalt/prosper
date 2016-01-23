(ns prosper.query
  (:require [clj-http.client :as http]
            [org.httpkit.client :as kit]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]))

(def base-url "https://api.sandbox.prosper.com/v1/")

(def access-token (atom nil))
(def refresh-token (atom nil))

(defn parse-body
  [{:keys [status body error]}]
  (if (= 200 status)
    (let [body' (json/parse-string body true)]
      (if-let [result (:result body')]
        result
        body'))
    (log/errorf
      "HTTP request received %s. Body is %s" status body)))

(defn update-tokens!
  [resp]
  (swap! access-token (constantly (:access_token resp)))
  (swap! refresh-token (constantly (:refresh_token resp))))

(defn request-access-token
  []
  (try
    (let [params {:grant_type "password"
                  :client_id (env :client-id)
                  :client_secret (env :client-secret)
                  :username (env :username)
                  :password (env :pass)}
          resp (-> (str base-url "security/oauth/token")
                   (http/post {:form-params params})
                   parse-body)]
      (update-tokens! resp)
      (log/infof "Received new access token: expires in %s" (:expires_in resp)))
    (catch Exception e
      (log/error
        (format "Caught exception while requesting access token: %s" e)))))

(defn refresh-access-token
  []
  (try
    (let [params {:grant_type "refresh_token"
                  :client_id (env :client-id)
                  :client_secret (env :client-secret)
                  :refresh_token @refresh-token}
          resp (-> (str base-url "security/oauth/token")
                   (http/post {:form-params params :oauth-token @access-token})
                   parse-body)]
      (update-tokens! resp)
      (log/infof "Refreshed access token. Expires in %s" (:expires_in resp)))
    (catch Exception e
      (log/error
        "Caught exception while refreshing access token. Requesting new token.")
      (request-access-token))))

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
      "HTTP request received %s. Body is %s" status body)))
