(ns prosper.query-test
  (:require [clojure.test :refer :all]
            [prosper.query :refer :all]))

(deftest test-request-token
  []
  (request-access-token)
  (let [new-access-token @access-token]
    (is (= 200 (:status @(kit-get "search/listings"))))
    (request-access-token)
    (let [final-access-token @access-token]
      (is (= 200 (:status @(kit-get "search/listings"))))
      (is (not= new-access-token final-access-token)))))

(deftest test-refresh-token
  []
  (request-access-token)
  (let [new-access-token @access-token]
    (is (= 200 (:status @(kit-get "search/listings"))))
    (refresh-access-token)
    (let [final-access-token @access-token]
      (is (= 200 (:status @(kit-get "search/listings"))))
      (is (not= new-access-token final-access-token)))))
