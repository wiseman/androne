(ns com.lemonodor.androne.fdl-test
  (:require
   [clojure.test :refer :all]
   [com.lemonodor.androne.fdl :as fdl]))


(deftest hierarchy-tests
  (let [w (-> {}
              (fdl/beget 'object 'animal)
              (fdl/beget 'animal 'cat))]
    (testing "beget/isa?"
      (is (fdl/isa? w 'animal 'object))
      (is (fdl/isa? w 'cat 'animal))
      (is (fdl/isa? w 'cat 'object)))
    (testing "all-abstractions-of"
      (is (= (fdl/all-abstractions-of w 'object) '(object)))
      (is (= (fdl/all-abstractions-of w 'animal) '(animal object)))
      (is (= (fdl/all-abstractions-of w 'cat) '(cat animal object))))))


(deftest slot-tests
  (testing "put-slot"
    (is (= (fdl/put-slot {} 'john 'name "john")
           {'john {'name "john"}}))
    (is (= (-> {}
               (fdl/put-slot 'john 'name "john")
               (fdl/put-slot 'john 'age 1)
               (fdl/put-slot 'amy 'name "amy"))
           {'john {'name "john"
                   'age 1}
            'amy {'name "amy"}})))
  (testing "get-slot"
    (let [w (-> {}
                (fdl/put-slot 'john 'name "john")
                (fdl/put-slot 'john 'age 1)
                (fdl/put-slot 'amy 'name "amy"))]
      (is (= (fdl/get-slot w 'john 'name) "john"))
      (is (= (fdl/get-slot w 'john 'age) 1))
      (is (= (fdl/get-slot w 'amy 'name) "amy"))))
  (testing "get-slot with inheritance"
    (let [w (-> {}
                (fdl/put-slot 'human 'size 'medium)
                (fdl/beget 'human 'john)
                (fdl/beget 'human 'amy)
                (fdl/put-slot 'amy 'size 'small))]
      (is (= (fdl/get-slot w 'john 'size) 'medium))
      (is (= (fdl/get-slot w 'amy 'size) 'small)))))


(deftest defworld-test
  (testing "simple defworld expansion"
    (let [w (fdl/defworld
              [animal
               :parent object
               eats food]
              [dog
               :parent animal
               likes food
               dislikes cat
               :phrases
               [[dog]
                [fido]
                [good dog]]])]
      (is (= (fdl/get-slot w 'dog 'likes) 'food))
      (is (= (fdl/get-slot w 'dog :phrases) '([dog] [fido] [good dog])))
      (is (= (fdl/get-slot w 'dog 'eats) 'food)))))
