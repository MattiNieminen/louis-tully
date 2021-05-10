(ns louis-tully.csv-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [louis-tully.csv :as csv])
  (:import (java.time LocalDateTime)))

(deftest csv-row->transaction-test
  (is (= {:ts (LocalDateTime/parse "2021-04-25T17:11:15")
          :from-ticker :btc
          :from-amount 0.7M
          :to-ticker :eth
          :to-amount 12.34M}
         (csv/csv-row->transaction
          "2021-04-25T17:11:15,BTC,0.7,ETH,12.34"))))

(deftest read-transactions-test
  (let [transactions (-> "resources/example.csv" csv/read-transactions)]
    (is (= 10 (count transactions)))
    (is (= (LocalDateTime/parse "2019-01-01T00:00:00")
           (-> transactions first :ts)))
    (is (= :usd (-> transactions last :to-ticker)))))

(deftest write-transactions!-test
  (let [filename "example-with-profits.csv"
        file (io/file filename)
        transactions [{:ts (LocalDateTime/parse "2020-01-05T12:15:30")
                       :from-ticker :usd
                       :from-amount 1500M
                       :to-ticker :ada
                       :to-amount 123.45M
                       :profit -100.5M}
                      {:ts (LocalDateTime/parse "2019-01-01T00:00:00")
                       :from-ticker :eur
                       :from-amount 1234.5M
                       :to-ticker :btc
                       :to-amount 0.5M
                       :profit 500.45M}]
        _ (csv/write-transactions! filename transactions)
        rows (-> filename slurp (str/split #"\n"))]
    (is (= "Timestamp,From-ticker,From-amount,To-ticker,To-amount,Profit" (first rows)))
    (is (= "2019-01-01T00:00,EUR,1234.5,BTC,0.5,500.45" (second rows)))
    (io/delete-file filename true)))
