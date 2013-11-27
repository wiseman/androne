(ns com.lemonodor.androne.fdl)


(defn beget [world parent child]
  (assoc-in world [child ::prototype] parent))


(defn isa? [world child parent]
  (if (= child parent)
    true
    (if child
      (recur world (get-in world [child ::prototype]) parent)
      false)))


(defn all-abstractions-of [world concept]
  (lazy-seq
   (if (not concept)
     '()
     (cons concept
           (all-abstractions-of world
                                (get-in world [concept ::prototype]))))))


(defn get-slot [world concept slot-name]
  (if concept
    (if-let [v (get-in world [concept slot-name])]
      v
      (recur world (get-in world [concept ::prototype]) slot-name))
    nil))


(defn put-slot [world concept slot-name slot-value]
  (assoc-in world [concept slot-name] slot-value))


(defmacro defworld [& frames]
  `(define-world '~frames))


(defn apply-clause-parent [world concept parent]
  (beget world parent concept))


(def clause-handlers
  {:parent apply-clause-parent})


(defn apply-clause [world concept-name clause]
  (let [[clause-name clause-body] clause
        handler (clause-handlers clause-name)]
    (if handler
      (handler world concept-name clause-body)
      (do
        (put-slot world concept-name clause-name clause-body)))))


(defn apply-frame [world frame]
  (let [[name & clauses] frame]
    (if clauses
      (reduce #(apply-clause %1 name %2)
              world
              (apply hash-map clauses))
      world)))


(defn define-world [frames]
  (assert (seq frames))
  (reduce #(apply-frame %1 %2) {} frames))
