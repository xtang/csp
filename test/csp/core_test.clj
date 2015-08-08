(ns csp.core-test
  (:require [clojure.test :refer :all]
            [csp.core :refer :all]))

(deftest cota-test
  (testing "get words from text"
    (is (= (count (get-words "go channel async")) 3))))
