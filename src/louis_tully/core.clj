(ns louis-tully.core
  (:require [clojure.string :as str]
            [louis-tully.csv :as csv]
            [louis-tully.transactor :as transactor]
            [louis-tully.summary :as summary]))

(defn -main [& args]
  (with-precision 10
    (let [[filename fiat-currency tax-rate] args
          profits-filename (-> filename (str/replace #"\.csv$"
                                                     "-with-profits.csv"))
          fiat-currency (-> fiat-currency str/lower-case keyword)
          tax-rate (bigdec tax-rate)
          transactions (csv/read-transactions filename)
          {:keys [transactions portfolio]} (transactor/transact transactions
                                                                fiat-currency)
          yearly-profits (summary/yearly-profits transactions)]
      (println (format "Writing transactions with profits to %s.\n"
                       profits-filename))
      (csv/write-transactions! profits-filename transactions)

      (println "PORTFOLIO: ")
      (doseq [[currency total] (summary/portfolio-total portfolio)]
        (println (format "%S: %.5f" (-> currency name str/upper-case) total)))

      (println "\nAVERAGE PRICES OF CURRENT PORTFOLIO:")
      (doseq [[currency avg-price] (summary/avg-prices portfolio)]
        (println (format "%S: %.5f" (-> currency name str/upper-case) avg-price)))

      (println "\nYEARLY PROFITS:")
      (doseq [[year profit] yearly-profits]
        (println (format "%s: %.2f" year profit)))

      (println "\nYEARLY TAXES:")
      (doseq [[year taxes] (summary/yearly-taxes yearly-profits tax-rate)]
        (println (format "%s: %.3f" year taxes))))))
