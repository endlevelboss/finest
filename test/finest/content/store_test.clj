(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 4 (count posts)))
    (is (= ["second" "first" "hero-collection" "hero-creator"] (map :slug posts)))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 4 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (= #{"alpha" "beta"} (store/all-tags))))

(deftest articles-excludes-collections-and-creators
  (store/load-all! fixture-dir)
  (is (= #{"first" "second"} (set (map :slug (store/articles))))))

(deftest referencing-finds-posts-pointing-at-a-collection
  (store/load-all! fixture-dir)
  (is (= ["second"] (map :slug (store/referencing "hero-collection"))))
  (is (= [] (store/referencing "no-such-collection"))))

(deftest credited-on-finds-collections-crediting-a-creator
  (store/load-all! fixture-dir)
  (is (= ["hero-collection"] (map :slug (store/credited-on "hero-creator"))))
  (is (= [] (store/credited-on "no-such-creator"))))
