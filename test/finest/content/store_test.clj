(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 2 (count posts)))
    (is (= ["second" "first"] (map :slug posts)))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 2 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (= #{"alpha" "beta"} (store/all-tags))))
