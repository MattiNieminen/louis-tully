(ns louis-tully.summary)

(defn portfolio-total [portfolio]
  (reduce-kv (fn [acc currency currency-portfolio]
               (assoc acc
                      currency
                      (->> currency-portfolio
                           (map :amount)
                           (reduce +)
                           bigdec)))
             {}
             portfolio))

(defn weighted-average [weights values]
  (when (and (-> weights empty? not)
             (-> values empty? not)
             (every? some? weights)
             (every? some? values)
             (= (count weights) (count values)))
    (/ (reduce + (map * weights values))
       (reduce + weights))))

(defn avg-prices [portfolio]
  (reduce-kv (fn [acc currency currency-portfolio]
               (assoc acc
                      currency
                      (weighted-average
                       (map :amount currency-portfolio)
                       (map :price currency-portfolio))))
             {}
             portfolio))

(defn yearly-profits [transactions]
  (reduce (fn [acc {:keys [ts profit]}]
            (update acc (.getYear ts) (fnil + 0M 0M) profit))
          {}
          transactions))

(defn yearly-taxes [yearly-profits tax-rate]
  (reduce-kv (fn [acc k v]
               (assoc acc k (-> tax-rate bigdec (* v))))
             {}
             yearly-profits))
