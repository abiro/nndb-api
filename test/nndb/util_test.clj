(ns nndb.util-test
  (:require [clojure.test :refer :all]

            [nndb.util :refer :all]))

(deftest test-to-single-cardinality
  (let [m {:a "foo"
           "b" "bar"
           :c [2 1 0]
           :d {:e 3 :f [0 1 2]}
           :g (java.util.Arrays/asList (to-array (range 3)))}
        n {:a "foo"
           "b" "bar"
           :c 2
           :d {:e 3 :f [0 1 2]}
           :g 0}]
    (is (= (to-single-cardinality m)
           n))))

(deftest test-dissoc-except
  (is (= {:b 1} (dissoc-except {:a 0 :b 1 :c 2} :b)))
  (is (= {:b 1 :c 2} (dissoc-except {:a 0 :b 1 :c 2} :b :c))))

