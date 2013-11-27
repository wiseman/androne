(ns com.lemonodor.androne.cp
  (:require
   [clojure.contrib.seq :as seq]
   [com.lemonodor.androne.fdl :as fdl]))


(defrecord Reference [item start end])


(defn reference [world parser-state token start end]
  (let [parser-state (update-in parser-state :references
                                conj
                                (Reference. token start end))]
    (reduce (fn [state abst]
              (advance-predictions-on state abst))
            (all-abstractions-of item))))


(defn parse [world tokens]
  (let [parser-state {}]
    (loop [[idx token] (seq/indexed tokens)]
      (recur (reference world parser-state token idx idx)))))
