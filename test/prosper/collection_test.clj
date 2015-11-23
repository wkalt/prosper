(ns prosper.collection-test
  (:require [clojure.test :refer :all]
            [prosper.collection :refer :all]))

(deftest state-update
  []
  (let [market-state (atom {})
        listings [{:ListingNumber 123 :AmountRemaining 10 :ProsperRating "A" :LenderYield 9.4}
                  {:ListingNumber 234 :AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}
                  {:ListingNumber 345 :AmountRemaining 99 :ProsperRating "A" :LenderYield 9.4}]]
    (update-state listings market-state)
    (testing "state was updated"
      (is (= @market-state
             {123 {:AmountRemaining 10 :ProsperRating "A" :LenderYield 9.4}
              234 {:AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}
              345 {:AmountRemaining 99 :ProsperRating "A" :LenderYield 9.4}})))

    (testing "new listings update amountremaining"
      (let [listings' [{:ListingNumber 123 :AmountRemaining 5 :ProsperRating "A" :LenderYield 9.4}
                       {:ListingNumber 234 :AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}
                       {:ListingNumber 345 :AmountRemaining 99 :ProsperRating "A" :LenderYield 9.4}]]
        (update-state listings' market-state)
        (is (= @market-state
               {123 {:AmountRemaining 5 :ProsperRating "A" :LenderYield 9.4}
                234 {:AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}
                345 {:AmountRemaining 99 :ProsperRating "A" :LenderYield 9.4}}))))
    (testing "funded listings disappear from state"
      (let [listings'' [{:ListingNumber 234 :AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}]]
        (update-state listings'' market-state)
        (is (= @market-state
               {234 {:AmountRemaining 94 :ProsperRating "A" :LenderYield 9.4}}))))))
