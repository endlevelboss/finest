(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 3 (count posts)))
    (is (= ["second" "first" "hero-comic"] (map :slug posts)))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 3 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (= #{"alpha" "beta"} (store/all-tags))))

(deftest articles-excludes-comics
  (store/load-all! fixture-dir)
  (is (= #{"first" "second"} (set (map :slug (store/articles))))))

(deftest referencing-finds-posts-pointing-at-a-comic
  (store/load-all! fixture-dir)
  (is (= ["second"] (map :slug (store/referencing "hero-comic"))))
  (is (= [] (store/referencing "no-such-comic"))))
