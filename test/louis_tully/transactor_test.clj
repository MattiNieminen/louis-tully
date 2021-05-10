(ns louis-tully.transactor-test
  (:require [clojure.test :refer :all]
            [louis-tully.transactor :as transactor]
            [louis-tully.price-history :as price-history])
  (:import (java.time LocalDateTime)))

(deftest fifo-buy-test
  (is (-> (transactor/fifo-buy nil :a 5M 10M) :a vector?))
  (is (= {:a [{:amount 5M :price 10M}]} (transactor/fifo-buy nil :a 5M 10M)))
  (is (= {:a [{:amount 5M :price 10M}]
          :b [{:amount 1M :price 2M}
              {:amount 1.5M :price 2.5M}]}
         (transactor/fifo-buy {:a [{:amount 5M :price 10M}]
                               :b [{:amount 1M :price 2M}]}
                              :b
                              1.5M
                              2.5M)))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Cannot add to portfolio without specifying the amount"
       (transactor/fifo-buy nil :a nil 20M)))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Cannot add to portfolio without price"
       (transactor/fifo-buy nil :a 1.2M nil))))

(deftest fifo-sell-test
  (let [portfolio {:a [{:amount 1M :price 1M}
                       {:amount 2M :price 2M}
                       {:amount 2.5M :price 3M}]
                   :b [{:amount 1.5M :price 2.5M}]}
        rest-vec (comp vec rest)]
    (is (= {:portfolio (update portfolio :a rest-vec)
            :profit 2M}
           (transactor/fifo-sell portfolio :a 1M 3M)))
    (is (= {:portfolio (-> portfolio
                           (update :a rest-vec)
                           (update :a rest-vec))
            :profit -0.5M}
           (transactor/fifo-sell portfolio :a 3M 1.5M)))
    (is (= {:portfolio (assoc portfolio :a [])
            :profit 15.0M}
           (transactor/fifo-sell portfolio :a 5.5M 5M)))
    (is (= {:portfolio (-> portfolio
                           (update :a rest-vec)
                           (assoc-in [:a 0 :amount] 0.5M))
            :profit 3.5M}
           (transactor/fifo-sell portfolio :a 2.5M 3M)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Cannot remove from portfolio without specifying the amount"
         (transactor/fifo-sell portfolio :a nil 1.5M)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Cannot remove from portfolio without price"
         (transactor/fifo-sell portfolio :a 1.5 nil)))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Cannot remove from portfolio because portfolio for ticker is empty"
         (transactor/fifo-sell portfolio :b 2M 1M)))))

(deftest transact-fiat-to-non-fiat-test
  (let [ts (LocalDateTime/parse "2019-01-01T00:00:00")
        from-amount 1000M
        to-amount 0.1M
        price (/ from-amount to-amount)
        test-transaction {:ts ts
                          :from-ticker :usd
                          :from-amount from-amount
                          :to-ticker :btc
                          :to-amount to-amount}
        test-portfolio {:btc [{:amount 0.05M :price 5000M}]
                        :eth [{:amount 10M :price 400M}]}]
    (is (= {:transaction test-transaction
            :portfolio (update test-portfolio
                               :btc
                               conj
                               {:amount to-amount :price price})}
           (transactor/transact-fiat-to-non-fiat test-transaction
                                                 test-portfolio
                                                 :usd)))
    (is (= {:transaction test-transaction
            :portfolio (update test-portfolio
                               :btc
                               conj
                               {:amount to-amount
                                :price (* price
                                          (price-history/price-for-ticker
                                           :usd
                                           :eur
                                           ts))})}
           (transactor/transact-fiat-to-non-fiat test-transaction
                                                 test-portfolio
                                                 :eur)))))
(deftest transact-non-fiat-to-fiat-test
  (let [ts (LocalDateTime/parse "2019-01-01T00:00:00")
        to-amount 35000M
        test-transaction {:ts ts
                          :from-ticker :btc
                          :from-amount 0.7M
                          :to-ticker :usd
                          :to-amount to-amount}
        test-portfolio {:btc [{:amount 0.1M :price 5000M}
                              {:amount 0.5M :price 9000M}
                              {:amount 0.2M :price 12000M}]
                        :eth [{:amount 10M :price 400M}]}
        test-portfolio-after (assoc test-portfolio
                                    :btc
                                    [{:amount 0.1M :price 12000M}])
        initial-cost (+ (* 0.1M 5000M) (* 0.5M 9000M) (* 0.1M 12000M))]
    (is (= {:transaction (assoc test-transaction
                                :profit
                                (- to-amount initial-cost))
            :portfolio test-portfolio-after}
           (transactor/transact-non-fiat-to-fiat test-transaction
                                                 test-portfolio
                                                 :usd)))
    (is (= {:transaction (assoc test-transaction
                                :profit (- (* to-amount
                                              (price-history/price-for-ticker
                                               :usd
                                               :eur
                                               ts))
                                           initial-cost))
            :portfolio test-portfolio-after}
           (transactor/transact-non-fiat-to-fiat test-transaction
                                                 test-portfolio
                                                 :eur)))))

(deftest transact-non-fiat-to-non-fiat-test
  (let [ts (LocalDateTime/parse "2019-01-01T12:15:15")
        from-amount 0.7M
        test-transaction {:ts ts
                          :from-ticker :btc
                          :from-amount from-amount
                          :to-ticker :eth
                          :to-amount 12.34M}
        test-portfolio {:btc [{:amount 0.5M :price 5000M}
                              {:amount 1.5M :price 3000M}]
                        :eth [{:amount 10M :price 400M}]}
        initial-cost (+ (* 0.5M 5000M) (* 0.2M 3000M))]
    (is (= {:transaction (assoc test-transaction
                                :profit
                                (- (* from-amount
                                      (price-history/price-for-ticker :btc
                                                                      :usd
                                                                      ts))
                                   initial-cost))
            :portfolio {:btc [{:amount 1.3M :price 3000M}]
                        :eth [{:amount 10M :price 400M}
                              {:amount 12.34M
                               :price (price-history/price-for-ticker :eth
                                                                      :usd
                                                                      ts)}]}}
           (transactor/transact-non-fiat-to-non-fiat test-transaction
                                                     test-portfolio
                                                     :usd)))
    (is (= {:transaction (assoc test-transaction
                                :profit
                                (- (* from-amount
                                      (price-history/price-for-ticker :btc
                                                                      :eur
                                                                      ts))
                                   initial-cost))
            :portfolio {:btc [{:amount 1.3M :price 3000M}]
                        :eth [{:amount 10M :price 400M}
                              {:amount 12.34M
                               :price (price-history/price-for-ticker :eth
                                                                      :eur
                                                                      ts)}]}}
           (transactor/transact-non-fiat-to-non-fiat test-transaction
                                                     test-portfolio
                                                     :eur)))))

(deftest fiat?-test
  (is (false? (transactor/fiat? nil)))
  (is (false? (transactor/fiat? :btc)))
  (is (true? (transactor/fiat? :eur)))
  (is (true? (transactor/fiat? :usd))))

(deftest transact-f-test
  (is (= transactor/transact-fiat-to-non-fiat
         (transactor/transact-f {:from-ticker :usd :to-ticker :btc})))
  (is (= transactor/transact-fiat-to-non-fiat
         (transactor/transact-f {:from-ticker :eur :to-ticker :eth})))
  (is (= transactor/transact-non-fiat-to-fiat
         (transactor/transact-f {:from-ticker :eth :to-ticker :usd})))
  (is (= transactor/transact-non-fiat-to-fiat
         (transactor/transact-f {:from-ticker :eth :to-ticker :eur})))
  (is (= transactor/transact-non-fiat-to-non-fiat
         (transactor/transact-f {:from-ticker :ada :to-ticker :doge})))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Operation not supported"
       (transactor/transact-f {:from-ticker :usd :to-ticker :eur}))))

(deftest transact-test
  (let [ts-2019 (LocalDateTime/parse "2019-01-01T23:59:59")
        ts-2020 (LocalDateTime/parse "2020-01-01T00:00:00")
        transaction-1-from-amount 20000M
        transaction-1-to-amount 2M
        transaction-1 {:ts ts-2019
                       :from-ticker :usd
                       :from-amount transaction-1-from-amount
                       :to-ticker :btc
                       :to-amount transaction-1-to-amount}
        transaction-2-from-amount 2000M
        transaction-2-to-amount 10M
        transaction-2 {:ts ts-2019
                       :from-ticker :eur
                       :from-amount transaction-2-from-amount
                       :to-ticker :eth
                       :to-amount transaction-2-to-amount}
        transaction-3-from-amount 10M
        transaction-3-to-amount 5M
        transaction-3 {:ts ts-2020
                       :from-ticker :eth
                       :from-amount transaction-3-from-amount
                       :to-ticker :btc
                       :to-amount transaction-3-to-amount}
        transaction-4-from-amount 3M
        transaction-4-to-amount 150000M
        transaction-4 {:ts ts-2020
                       :from-ticker :btc
                       :from-amount transaction-4-from-amount
                       :to-ticker :usd
                       :to-amount transaction-4-to-amount}]
    (is (= {:transactions [] :portfolio {}}
           (transactor/transact [] :eur)))
    (is (= {:transactions [transaction-1
                           transaction-2
                           (assoc transaction-3
                                  :profit
                                  (- (* transaction-3-from-amount
                                        (price-history/price-for-ticker
                                         :eth
                                         :eur
                                         ts-2020))
                                     (* transaction-3-from-amount
                                        (/ transaction-2-from-amount
                                           transaction-2-to-amount))))
                           (assoc transaction-4
                                  :profit
                                  (- (* transaction-4-to-amount
                                        (price-history/price-for-ticker
                                         :usd
                                         :eur
                                         ts-2020))
                                     (+ (* transaction-1-from-amount
                                           (price-history/price-for-ticker
                                            :usd
                                            :eur
                                            ts-2019))
                                        (* (- transaction-4-from-amount
                                              transaction-1-to-amount)
                                           (price-history/price-for-ticker
                                            :btc
                                            :eur
                                            ts-2020)))))]
            :portfolio {:btc [{:amount (- (+ transaction-1-to-amount
                                             transaction-3-to-amount)
                                          transaction-4-from-amount)
                               :price (price-history/price-for-ticker
                                       :btc
                                       :eur
                                       ts-2020)}]
                        :eth []}}
           (with-precision 12
             (transactor/transact [transaction-1 transaction-2
                                   transaction-3 transaction-4]
                                  :eur))))))
