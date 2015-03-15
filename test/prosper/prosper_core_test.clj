(ns prosper.prosper-core-test
  (:require [clojure.test :refer :all]
            [prosper.prosper-core :refer :all]))

(deftest hello-test
  (testing "says hello to caller"
    (is (= "Hello, foo!" (hello "foo")))))
