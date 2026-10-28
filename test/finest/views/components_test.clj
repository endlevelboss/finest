(ns finest.views.components-test
  (:require [clojure.test :refer [deftest is]]
            [finest.views.components :as c]))

(defn- star-classes
  [rating]
  (->> (c/rating-stars rating)
       (tree-seq sequential? seq)
       (keep #(when (and (vector? %) (= :span.star (first %))) (:class (second %))))))

(deftest half-ratings-show-a-half-star
  (is (= ["filled" "filled" "half"] (star-classes 2.5)))
  (is (= ["half" "empty" "empty"] (star-classes 0.5)))
  (is (= ["filled" "filled" "filled"] (star-classes 3)))
  (is (= ["empty" "empty" "empty"] (star-classes 0)))
  (is (= ["filled" "filled" "empty"] (star-classes 2))))
