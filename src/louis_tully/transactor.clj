(ns louis-tully.transactor
  (:require [louis-tully.price-history :as price-history]))

(defn fifo-buy [portfolio ticker amount price]
  (when-not amount
    (throw (ex-info "Cannot add to portfolio without specifying the amount"
                    {:ticker ticker})))
  (when-not price
    (throw (ex-info "Cannot add to portfolio without price"
                    {:ticker ticker})))
  (update portfolio
          ticker
          #(conj (or %1 []) %2)
          {:amount amount
           :price price}))


(defn fifo-sell [portfolio ticker amount price]
  (when-not amount
    (throw (ex-info "Cannot remove from portfolio without specifying the amount"
                    {:ticker ticker})))
  (when-not price
    (throw (ex-info "Cannot remove from portfolio without price"
                    {:ticker ticker})))
  (loop [portfolio portfolio
         amount-left amount
         original-cost 0]
    (let [portfolio-first (-> portfolio (get ticker) first)
          portfolio-first-amount (:amount portfolio-first)
          portfolio-first-price (:price portfolio-first)]
      (cond
        (and (> amount-left 0) (nil? portfolio-first))
        (throw (ex-info "Cannot remove from portfolio because portfolio for ticker is empty"
                        {:ticker ticker}))

        (and (> amount-left 0) (>= amount-left portfolio-first-amount))
        (recur (update portfolio ticker (comp vec rest))
               (- amount-left portfolio-first-amount)
               (+ original-cost (* portfolio-first-amount
                                   portfolio-first-price)))

        :else
        {:portfolio (if portfolio-first
                      (assoc-in portfolio
                                [ticker 0 :amount]
                                (- portfolio-first-amount amount-left))
                      portfolio)
         :profit (- (* amount price)
                    (+ original-cost
                       (if portfolio-first-price
                         (* amount-left portfolio-first-price)
                         0)))}))))

(defn transact-fiat-to-non-fiat [{:keys [ts from-ticker from-amount to-ticker
                                         to-amount] :as transaction} portfolio
                                 fiat]
  (let [price (/ (if (= from-ticker fiat)
                   from-amount
                   (* from-amount (price-history/price-for-ticker
                                   from-ticker
                                   fiat
                                   ts)))
                 to-amount)]
    {:transaction transaction
     :portfolio (fifo-buy portfolio to-ticker to-amount price)}))

(defn transact-non-fiat-to-fiat [{:keys [ts from-ticker from-amount to-ticker
                                         to-amount] :as transaction} portfolio
                                 fiat]
  (let [price (/ (if (= to-ticker fiat)
                   to-amount
                   (* to-amount (price-history/price-for-ticker
                                 to-ticker
                                 fiat
                                 ts)))
                 from-amount)
        {:keys [portfolio profit]} (fifo-sell portfolio
                                              from-ticker
                                              from-amount
                                              price)]
    {:transaction (assoc transaction :profit profit)
     :portfolio portfolio}))

(defn transact-non-fiat-to-non-fiat [{:keys [ts from-ticker from-amount to-ticker
                                             to-amount] :as transaction}
                                     portfolio fiat]
  (let [from-ticker-price (price-history/price-for-ticker from-ticker
                                                          fiat
                                                          ts)
        to-ticker-price (price-history/price-for-ticker to-ticker
                                                        fiat
                                                        ts)
        portfolio (fifo-buy portfolio to-ticker to-amount to-ticker-price)
        {:keys [portfolio profit]} (fifo-sell portfolio
                                              from-ticker
                                              from-amount
                                              from-ticker-price)]
    {:transaction (assoc transaction :profit profit)
     :portfolio portfolio}))

(defn fiat? [ticker]
  (contains? #{:usd :eur} ticker))

(defn transact-f [{:keys [from-ticker to-ticker]}]
  (let [from-fiat? (fiat? from-ticker)
        to-fiat? (fiat? to-ticker)]
    (cond
      (and from-fiat? to-fiat?)
      (throw (ex-info "Operation not supported" {}))

      (and from-fiat? (not to-fiat?))
      transact-fiat-to-non-fiat

      (and (not from-fiat?) to-fiat?)
      transact-non-fiat-to-fiat

      (and (not from-fiat?) (not to-fiat?))
      transact-non-fiat-to-non-fiat)))

(defn transact [transactions fiat]
  (reduce (fn [{:keys [portfolio] :as acc} transaction]
            (let [transact-f (transact-f transaction)
                  {:keys [transaction portfolio]} (transact-f transaction
                                                              portfolio
                                                              fiat)]
              (-> acc
                  (update :transactions conj transaction)
                  (assoc :portfolio portfolio))))
          {:transactions []
           :portfolio {}}
          transactions))
