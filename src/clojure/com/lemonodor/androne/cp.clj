(ns com.lemonodor.androne.cp
  (:require
   [com.lemonodor.androne.fdl :as fdl]
   [instaparse.core :as insta]
   [taoensso.timbre :as log]
   ))


(def phrasal-pattern-parser
  (insta/parser
   "<pattern> = term (<#'\\s+'> term)*
    <term> = sequence | optional | choice | slot_reference | word
    sequence = <'['> term (<#'\\s+'> term)* <']'>
    optional = <'?:'> term
    choice = <'['> term (<#'\\s*'> <'|'> <#'\\s*'> term)* <']'>
    slot_reference = <'{'> #'\\w+' <'}'>
    word = #'[\\pL\\pM\\p{Nd}\\p{Nl}\\p{Pc}[\\p{InEnclosedAlphanumerics}&&\\p{So}]]+'"))

(defn parse-phrasal-pattern [pattern]
  (let [p (phrasal-pattern-parser pattern)]
    (if (> (count p) 1)
      (vec (cons :sequence p))
      (first p))))


(defrecord Prediction [base phrasal-pattern start next slots value])

(defrecord Parser
    [world predictions])


(defn index-prediction [parser prediction-type prediction]
  (update-in parser [:predictions prediction-type (:base prediction)] conj prediction))

(defn word-prediction-generator [base phrasal-pattern start position slots]
  (list (map->Prediction
         {:base base
          :phrasal-pattern (second phrasal-pattern)
          :start start
          :position position
          :slots slots})))

(def syntax-functions
  {:word word-prediction-generator})

(defn generate-predictions [concept phrasal-pattern start position slots]
  (if-let [syntax-function (syntax-functions (first phrasal-pattern))]
    (let [predictions (apply
                       syntax-function
                       concept phrasal-pattern start position slots
                       '())]
      (log/info "Predictions on" phrasal-pattern ":" predictions)
      predictions)
    (throw (Exception. (str "Unknown synax directive for concept "
                            concept ": " phrasal-pattern)))))

(defn add-phrasal-pattern [parser concept pattern]
  (log/info "Adding phrasal pattern to parser" parser
            "for concept" concept
            "with pattern" pattern)
  (reduce (fn [parser prediction]
            (index-prediction parser :anytime prediction))
          parser
          (generate-predictions
           concept (parse-phrasal-pattern pattern) nil nil {})))

(defn add-phrasal-patterns-for-concept [parser concept slots]
  (log/info "Adding phrasal patterns to" parser
            "for concept" concept
            "with slots" slots)
  (reduce (fn [parser phrase]
            (add-phrasal-pattern parser concept phrase))
          parser
          (slots :phrases)))

(defn add-phrasal-patterns [parser]
  (log/info "Adding phrasal patterns to" parser)
  (reduce (fn [parser [concept slots]]
            (add-phrasal-patterns-for-concept parser concept slots))
          parser
          (:world parser)))


(defn predictions-on [parser concept]
  (concat (get-in parser [:predictions :anytime concept])
          (get-in parser [:predictions :dynamic concept])))


(defrecord Reference [item start end])


(defn parser [world]
  (-> (map->Parser {:world world
                    :predictions {}})
      add-phrasal-patterns))

;; (defn reference [world parser-state token start end]
;;   (let [parser-state (update-in parser-state :references
;;                                 conj
;;                                 (Reference. token start end))]
;;     (reduce advance-predictions-on
;;             (all-abstractions-of item))))


;; (defn parse [world tokens]
;;   (let [parser-state {}]
;;     (loop [[idx token] (seq/indexed tokens)]
;;       (recur (reference world parser-state token idx idx)))))
