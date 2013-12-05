(ns com.lemonodor.androne.icp
  (:use
   [clojure.set :as set]))


(defn index-concept [concept frame]
  (apply merge-with set/union
         (map #(into {} (map (fn [word] [(str word) #{concept}]) %1))
              (frame :index-sets))))


(defn index-concepts [world]
  (apply merge-with set/union
         (map #(apply index-concept %1) world)))


(defn lookup-concepts [concept-index tokens]
  (set
   (reduce concat
           (map #(get concept-index %1)
                tokens))))


(defn target-concept-cardinality [concept-index word]
  (count (concept-index word)))


(defn unique-target-concepts [concept-index]
  (reduce set/union (vals concept-index)))


(defn probability-of-word [concept-index word]
  (let [cardinality (target-concept-cardinality concept-index word)]
    (if (zero? cardinality)
      Float/MIN_VALUE
      (/ cardinality (count (unique-target-concepts concept-index))))))


(defn log2 [n]
  (/ (Math/log n) (Math/log 2)))


(defn information-value [concept-index word]
  (let [v (- (log2 (probability-of-word concept-index word)))]
    (if (zero? v)
      Float/MIN_VALUE
      v)))


(defn summed-value [concept-index words]
  (reduce + (map #(information-value concept-index %1) words)))


(defn predicted-items [predictions seen]
  (intersection (set predictions) (set seen)))


(defn predicted-score [concept-index index-set found]
  (let [predicted-and-seen (intersection (set index-set) (set found))]
    (/ (summed-value concept-index predicted-and-seen)
       (summed-value concept-index index-set))))

(defn unpredicted-score [concept-index index-set found]
  (let [unpredicted-and-seen (set/difference (set found) (set index-set))]
    (- 1.0
       (/ (summed-value concept-index unpredicted-and-seen)
          (summed-value concept-index found)))))


(defn score-index-set [concept-index index-set words]
  (+ (* 1.0 (predicted-score concept-index index-set words))
     (* 1.0 (unpredicted-score concept-index index-set words))))


(defn score-index-sets [concept-index world concepts words]
  (map (fn [concept]
         (first
          (sort-by
           last
           >
           (map (fn [index-set]
                  [concept (score-index-set
                            concept-index
                            (map str index-set)
                            words)])
                (get-in world [concept :index-sets])))))
       concepts))


(defn icp [world words]
  (let [concept-index (index-concepts world)
        concepts (lookup-concepts concept-index words)]
    (sort-by last
             >
             (score-index-sets concept-index world concepts words))))
