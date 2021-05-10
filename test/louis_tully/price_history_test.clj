(ns louis-tully.price-history-test
  (:require [clojure.test :refer :all]
            [louis-tully.price-history :as price-history])
  (:import (java.time LocalDate LocalDateTime)))

(def date-2018 (LocalDate/parse "2018-12-31"))
(def date-2019 (LocalDate/parse "2019-01-01"))
(def date-2020 (LocalDate/parse "2020-10-13"))

(def ts-2019 (LocalDateTime/parse "2019-01-01T01:00:00"))
(def ts-2020 (LocalDateTime/parse "2020-10-13T12:01:01"))

(deftest yahoo-csv->price-history-test
  (let [eur-usd (get price-history/price-histories "eur-usd")
        btc-usd (get price-history/price-histories "btc-usd")]
    (is (nil? (get eur-usd date-2018)))
    (is (nil? (get btc-usd date-2018)))
    (is (= 1.149306M (get eur-usd date-2019)))
    (is (= 3843.520020M (get btc-usd date-2019)))
    (is (= 1.181614M (get eur-usd date-2020)))
    (is (= 11384.181641M (get btc-usd date-2020)))
    (is (= 1.102913M (get eur-usd (LocalDate/parse "2020-02-01"))))))

(deftest combined-price-history-test
  (let [ada-eur (get price-history/price-histories "ada-eur")
        btc-eur (get price-history/price-histories "btc-eur")]
    (is (nil? (get ada-eur date-2018)))
    (is (nil? (get btc-eur date-2018)))
    (is (= 0.03701973191M (get ada-eur date-2019)))
    (is (= 3344.209479M (get btc-eur date-2019)))
    (is (= 0.09016311587M (get ada-eur date-2020)))
    (is (= 9634.433615M (get btc-eur date-2020)))))

(deftest reversed-price-history-test
  (let [usd-eur (get price-history/price-histories "usd-eur")]
    (is (nil? (get usd-eur date-2018)))
    (is (= 0.8700902980M (get usd-eur date-2019)))
    (is (= 0.8463000608M (get usd-eur date-2020)))))

(deftest filename->pair-test
  (is (nil? (price-history/filename->pair nil)))
  (is (nil? (price-history/filename->pair "")))
  (is (= ["eur" "usd"] (price-history/filename->pair "EuR-USd.Csv"))))

(deftest price-for-ticker-test
  (is (nil? (price-history/price-for-ticker :nonexisting :eur ts-2020)))
  (is (= 1 (price-history/price-for-ticker :foo :foo ts-2019)))
  (is (= 1 (price-history/price-for-ticker :usd :usdt ts-2019)))
  (is (= 0.03701973191M (price-history/price-for-ticker :ada :eur ts-2019)))
  (is (= 0.042547M (price-history/price-for-ticker :ada :usd ts-2019)))
  (is (= 375.142059M (price-history/price-for-ticker :eth :usd ts-2020)))
  (is (= 9634.433615M (price-history/price-for-ticker :btc :eur ts-2020)))
  (is (= 1.149306M (price-history/price-for-ticker :eur :usd ts-2019)))
  (is (= 0.1008695682M (price-history/price-for-ticker :xlm :eur ts-2019)))
  (is (= 0.115930M (price-history/price-for-ticker :xlm :usd ts-2019)))
  (is (= 0.115930M (price-history/price-for-ticker :xlm :usdt ts-2019))))
