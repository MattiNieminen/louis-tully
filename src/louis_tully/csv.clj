(ns louis-tully.csv
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.time LocalDateTime)))

(defn csv-row->transaction [csv-line]
  (let [[ts from-ticker from-amount to-ticker to-amount] (str/split csv-line
                                                                    #",")]
    {:ts (LocalDateTime/parse ts)
     :from-ticker (-> from-ticker str/lower-case keyword)
     :from-amount (bigdec from-amount)
     :to-ticker (-> to-ticker str/lower-case keyword)
     :to-amount (bigdec to-amount)}))

(defn read-transactions [filename]
  (with-open [rdr (-> filename io/file clojure.java.io/reader)]
    (->> rdr
         line-seq
         rest
         (map csv-row->transaction)
         (sort-by :ts))))

(defn write-transactions! [filename transactions]
  (io/delete-file filename true)
  (with-open [w (clojure.java.io/writer filename :append true)]
    (.write w "Timestamp,From-ticker,From-amount,To-ticker,To-amount,Profit\n")
    (doseq [{:keys [ts from-ticker from-amount to-ticker to-amount profit]}
            (sort-by :ts transactions)]
      (.write w (format "%s,%s,%s,%s,%s,%s\n"
                        ts
                        (-> from-ticker name str/upper-case)
                        from-amount
                        (-> to-ticker name str/upper-case)
                        to-amount
                        profit)))))
