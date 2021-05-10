(ns louis-tully.price-history
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.time LocalDate)))

(defn yahoo-csv->price-history* [file]
  (with-open [rdr (io/reader file)]
    (->> rdr
         line-seq
         rest
         (map #(str/split % #","))
         (reduce (fn [acc [date _ _ _ _ adj-close _]]
                   (assoc acc
                          (LocalDate/parse date)
                          (try
                            (bigdec adj-close)
                            (catch java.lang.NumberFormatException e nil))))
                 {}))))

(defn yahoo-csv->price-history [file]
  (let [price-history (yahoo-csv->price-history* file)
        dates (-> price-history keys sort)
        min-date (first dates)
        max-date (last dates)]
    (loop [price-history price-history
           date min-date
           last-price nil]
      (let [price (or (get price-history date) last-price)]
        (if (= date max-date)
          price-history
          (recur (assoc price-history date price)
                 (.plusDays date 1)
                 price))))))

(defn combined-price-history [op price-history-1 price-history-2]
  (with-precision 10
    (->> (keys price-history-1)
         (select-keys price-history-2)
         keys
         (reduce (fn [acc date]
                   (assoc acc date (op (get price-history-1 date)
                                       (get price-history-2 date))))
                 {}))))

(defn reversed-price-history [price-history]
  (with-precision 10
    (reduce-kv (fn [acc date price]
                 (assoc acc date (/ 1 price)))
               {}
               price-history)))

(defn filename->pair [filename]
  (let [pair (some-> filename
                     str/lower-case
                     (str/replace #"\..*" "")
                     (str/split #"-"))]
    (when (and pair (vector? pair) (= (count pair) 2))
      pair)))

(def eur-usd (-> "resources/yahoo-csv/EUR-USD.csv"
                 io/file
                 yahoo-csv->price-history))

(def usd-eur (reversed-price-history eur-usd))

(def non-eur-files
  (->> "resources/yahoo-csv/"
       io/file
       file-seq
       (filter (fn [file]
                 (let [filename (-> file .getName str/lower-case)]
                   (and (str/ends-with? filename ".csv")
                        (-> filename (str/starts-with? "eur") not)))))))

(def price-histories-from-yahoo-csvs
  (reduce (fn [acc file]
            (assoc acc
                   (->> file .getName filename->pair (str/join #"-"))
                   (yahoo-csv->price-history file)))
          {}
          non-eur-files))

(def eur-price-histories
  (reduce-kv (fn [acc pair price-history]
               (assoc acc
                      (-> pair (str/split #"-") first (str "-eur"))
                      (combined-price-history / price-history eur-usd)))
          {}
          price-histories-from-yahoo-csvs))

(def price-histories (merge price-histories-from-yahoo-csvs
                            eur-price-histories
                            {"eur-usd" eur-usd
                             "usd-eur" usd-eur}))

(defn price-for-ticker [ticker fiat ts]
  (let [ticker (if (= ticker :usdt) :usd ticker)
        fiat (if (= fiat :usdt) :usd fiat)]
    (if (= ticker fiat)
      1
      (get-in price-histories [(str (name ticker) "-" (name fiat))
                               (.toLocalDate ts)]))))
