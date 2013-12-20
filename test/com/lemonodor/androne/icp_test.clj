(ns com.lemonodor.androne.icp-test
  (:require
   [clojure.math.numeric-tower :as math]
   [clojure.test :refer :all]
   [com.lemonodor.androne.fdl :as fdl]
   [com.lemonodor.androne.icp :as icp]))


(def epsilon (* 2 Float/MIN_VALUE))


(defn nearly= [a b]
  (< (math/abs (- a b)) epsilon))


(defn parse-results= [results1 results2]
  (every? (fn [[r1 r2]]
            (and (= (first r1) (first r2))
                 (nearly= (second r1) (second r2))))
          (map vector results1 results2)))


(deftest icp-tests
  (let [w (fdl/defworld
            [move-forward
             :index-sets [[please move forward]]]
            [move-backward
             :index-sets [[please move backward]]]
            [take-off
             :index-sets [[please take off] [please takeoff]]])
        idx (icp/index-concepts w)]
    (testing "index-concepts"
      (is (= idx
             {"backward" #{'move-backward},
              "forward" #{'move-forward},
              "move" #{'move-forward 'move-backward},
              "off" #{'take-off},
              "please" #{'move-forward 'take-off 'move-backward},
              "take" #{'take-off},
              "takeoff" #{'take-off}}))
      (is (= (icp/unique-target-concepts idx)
             #{'move-backward 'move-forward 'take-off})))
    (testing "probabilities"
      (is (= (icp/probability-of-word idx "move") 2/3))
      (is (= (icp/probability-of-word idx "forward") 1/3))
      (is (nearly= (icp/probability-of-word idx "blue") 0)))
    (testing "information values"
      (is (= (icp/information-value idx "move") 0.5849625007211561))
      (is (= (icp/information-value idx "forward") 1.5849625007211563))
      (is (nearly= (icp/information-value idx "please") 0)))
    (testing "scoring"
      (is (= (icp/predicted-score idx ["please" "take" "off"] ["please" "take"])
             0.5))
      (is (= (icp/predicted-score idx
                                  ["please" "take" "off"]
                                  ["please" "take" "off"])
             1.0))
      (is (= (icp/unpredicted-score idx
                                    ["please" "take" "off"]
                                    ["please" "take" "off"])
             1.0))
      (is (= (icp/unpredicted-score idx
                                    ["please" "take" "off"]
                                    ["please" "take" "blue" "off"])
             0.02083148165718207))
      (is (= (icp/score-index-set idx
                                  ["please" "take" "off"]
                                  ["please" "take"])
             1.5))
      (is (= (icp/score-index-set idx
                                  ["please" "take" "off"]
                                  ["please" "take" "off"])
             2.0))
      (is (= (icp/score-index-sets idx
                                   w
                                   #{'take-off}
                                   ["please" "take"])
             '([take-off 1.5]))))
    (testing "icp"
      (is (parse-results=
           (icp/icp w ["please" "take"])
           '([take-off 1.5] [move-forward 0] [move-backward 0])))
      (is (parse-results=
           (icp/icp w ["please" "take"])
           '([take-off 1.5] [move-forward 0] [move-backward 0])))
      (is (parse-results=
           (icp/icp w ["please"])
           '([move-forward 1.0] [take-off 1.0] [move-backward 1.0])))
      (is (parse-results=
           (icp/icp w ["please" "take" "off"])
           '([take-off 2.0] [move-forward 0] [move-backward 0])))
      (is (parse-results=
           (icp/icp w ["please" "take" "blue" "off"])
           '([take-off 1.020831481657182]
             [move-forward 0]
             [move-backward 0])))
      (is (= (icp/icp w ["NOOOOO"]) '())))))

(deftest world-test
  (let [w (fdl/defworld
            [take-off
             :index-sets [[take off]
                          [takeoff]]
             :action do-take-off]
            [land
             :index-sets [[land] [abort] [emergency]]
             :action do-land]
            [forward
             :parent relative-direction
             :index-sets [[forward]]]
            [backward
             :parent relative-direction
             :index-sets [[backward]]])]
    (testing "parsing"
      (is (parse-results= (icp/icp w '("land"))
                          '([land 2.0]))))))
