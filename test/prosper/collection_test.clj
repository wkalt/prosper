(ns prosper.collection-test
  (:require [clojure.test :refer :all]
            [prosper.query :as query]
            [environ.core :refer [env]]
            [clj-time.core :refer [now plus minutes minus]]
            [prosper.collection :refer :all]
            [prosper.storage :as storage]
            [clj-time.core :refer [now minutes]]))

(deftest state-update
  []
  (let [market-state (atom {})
        listings [{:listing_number 123 :amount_remaining 10 :prosper_rating "A" :lender_yield 9.4}
                  {:listing_number 234 :amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
                  {:listing_number 345 :amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}]]
    (update-state! listings market-state)
    (testing "state was updated"
      (is (= @market-state
             {123 {:amount_remaining 10 :prosper_rating "A" :lender_yield 9.4}
              234 {:amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
              345 {:amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}})))

    (testing "new listings update amountremaining"
      (let [listings' [{:listing_number 123 :amount_remaining 5 :prosper_rating "A" :lender_yield 9.4}
                       {:listing_number 234 :amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
                       {:listing_number 345 :amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}]]
        (update-state! listings' market-state)
        (is (= @market-state
               {123 {:amount_remaining 5 :prosper_rating "A" :lender_yield 9.4}
                234 {:amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
                345 {:amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}}))))
    (testing "funded listings do not disappear from state"
      (let [listings'' [{:listing_number 234 :amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}]]
        (update-state! listings'' market-state)
        (is (= @market-state
               {123 {:amount_remaining 5 :prosper_rating "A" :lender_yield 9.4}
                234 {:amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
                345 {:amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}}))))

    (testing "new listings appear in state"
      (let [listings''' [{:listing_number 789 :amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}]]
        (update-state! listings''' market-state)
        (is (= @market-state
               {234 {:amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}
                123 {:amount_remaining 5 :prosper_rating "A" :lender_yield 9.4}
                345 {:amount_remaining 99 :prosper_rating "A" :lender_yield 9.4}
                789 {:amount_remaining 94 :prosper_rating "A" :lender_yield 9.4}}))))

    (testing "prune state clears state"
      (let [{:keys [client-id client-secret username password base-url]} (env :prosper)]
        (query/request-access-token client-id client-secret username password base-url)
        (prune-market-state! market-state "search/listings" base-url)
        (is (= @market-state {}))))))

(deftest in-release-test
  []
  (with-redefs [storage/release-end-time (atom (plus (now) (minutes 15)))]
    (is (true? (in-release?)))
    (reset! storage/release-end-time (minus (now) (minutes 1)))
    (is (false? (in-release?)))))
