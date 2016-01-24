(ns prosper.query-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [prosper.query :refer :all]))

(deftest test-request-token
  []
  (let [{:keys [client-id client-secret username password base-url]} (env :prosper)
        _ (request-access-token client-id client-secret username password base-url)
        new-access-token @access-token]
    (is (= 200 (:status @(kit-get "search/listings" base-url))))
    (request-access-token client-id client-secret username password base-url)
    (let [final-access-token @access-token]
      (is (= 200 (:status @(kit-get "search/listings" base-url))))
      (is (not= new-access-token final-access-token)))))

(deftest test-refresh-token
  []
  (let [{:keys [client-id client-secret username password base-url]} (env :prosper)
        _ (request-access-token client-id client-secret username password base-url)
        new-access-token @access-token]
    (is (= 200 (:status @(kit-get "search/listings" base-url))))
    (refresh-access-token client-id client-secret username password base-url)
    (let [final-access-token @access-token]
      (is (= 200 (:status @(kit-get "search/listings" base-url))))
      (is (not= new-access-token final-access-token)))))
