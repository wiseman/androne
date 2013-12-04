(ns com.lemonodor.androne.icp-test
  (:require
   [clojure.math.numeric-tower :as math]
   [clojure.test :refer :all]
   [com.lemonodor.androne.fdl :as fdl]
   [com.lemonodor.androne.icp :as icp]))


(deftest icp-tests
  (let [w (fdl/defworld
            [move-forward
             :index-sets [["please" "move" "forward"]]]
            [move-backward
             :index-sets [["please" "move" "backward"]]]
            [take-off
             :index-sets [["please" "take" "off"] ["please" "takeoff"]]])
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
      (is (< (math/abs (icp/probability-of-word idx "blue")) 0.001)))
    (testing "information values"
      (is (= (icp/information-value idx "move") 0.5849625007211561))
      (is (= (icp/information-value idx "forward") 1.5849625007211563))
      (is (zero? (icp/information-value idx "please"))))
    (testing "scoring"
      (is (= (icp/predicted-score idx ["please" "take" "off"] ["please" "take"])
             0.5))
      (is (= (icp/predicted-score
              idx
              ["please" "take" "off"]
              ["please" "take" "off"])
             1.0))
      (is (= (icp/unpredicted-score
              idx
              ["please" "take" "off"]
              ["please" "take" "off"])
             1.0))
      (is (= (icp/unpredicted-score
              idx
              ["please" "take" "off"]
              ["please" "take" "blue" "off"])
             0.02083148165718207))
      (is (= (icp/score-index-set
              idx
              ["please" "take" "off"]
              ["please" "take"])
             1.5))
      (is (= (icp/score-index-set
              idx
              ["please" "take" "off"]
              ["please" "take" "off"])
             2.0))
      (is (= (icp/score-index-sets
              idx
              w
              #{'take-off}
              ["please" "take"])
             nil)))
    (testing "icp"
      (is (= (icp/icp w ["please" "take"])
             nil))
      (is (= (icp/icp w ["please"])
             nil))
      (is (= (icp/icp w ["please" "take" "off"])
             nil))
      (is (= (icp/icp w ["please" "take" "blue" "off"])
             nil)))))
