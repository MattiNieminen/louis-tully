(ns louis-tully.summary-test
  (:require [clojure.test :refer :all]
            [louis-tully.summary :as summary])
  (:import (java.time LocalDateTime)))

(deftest portfolio-total-test []
  (is (= {} (summary/portfolio-total {})))
  (is (= {:a 0M} (summary/portfolio-total {:a []})))
  (is (= {:a 6.54M :b 3.001M}
         (summary/portfolio-total {:a [{:amount 1.2M :price 2M}
                                       {:amount 2.34M :price 4M}
                                       {:amount 3M :price 6M}]
                                   :b [{:amount 1.001M :price 3M}
                                       {:amount 1M}
                                       {:amount 1M :price 3M}]}))))

(deftest weighted-average-test
  (with-precision 2
    (is (nil? (summary/weighted-average [] [])))
    (is (= 6.3M (summary/weighted-average [1M 2M 3M] [5M 6M 7M])))
    (is (= 6.3M (summary/weighted-average [1M 2M 3M] [5M 6M 7M])))
    (is (nil? (summary/weighted-average [1M 2M 3M] [5M 6M])))
    (is (nil? (summary/weighted-average [1M nil 3M] [5M 6M 7M])))
    (is (nil? (summary/weighted-average [1M 2M 3M] [5M 6M nil])))))

(deftest avg-prices-test
  (is (= {} (summary/avg-prices {})))
  (is (= {:a 4.7M :b nil}
         (with-precision 2
           (summary/avg-prices {:a [{:amount 1M :price 2M}
                                    {:amount 2M :price 4M}
                                    {:amount 3M :price 6M}]
                                :b [{:amount 1M :price 3M}
                                    {:amount 1M}
                                    {:amount 1M :price 3M}]})))))

(deftest yearly-profits-test
  (is (= {} (summary/yearly-profits [])))
  (is (= {2018 0M 2019 100M 2020 200M 2021 1500M}
         (summary/yearly-profits
          [{:ts (LocalDateTime/parse "2018-12-31T00:00:00")}
           {:ts (LocalDateTime/parse "2019-03-01T00:00:00") :profit 100M}
           {:ts (LocalDateTime/parse "2020-01-15T00:00:00") :profit -100M}
           {:ts (LocalDateTime/parse "2020-05-20T00:00:00") :profit 300M}
           {:ts (LocalDateTime/parse "2021-01-01T00:00:00") :profit -2000M}
           {:ts (LocalDateTime/parse "2021-01-01T00:00:00") :profit 3500M}
           {:ts (LocalDateTime/parse "2021-01-01T00:00:00")}]))))

(deftest yearly-taxes-test
  (is (= {} (summary/yearly-taxes {} 0.3)))
  (is (= {2018 0M 2019 30M 2020 60M 2021 450M}
         (summary/yearly-taxes {2018 0M 2019 100M 2020 200M 2021 1500M} 0.3))))
