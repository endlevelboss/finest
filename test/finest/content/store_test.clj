(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 6 (count posts)))
    (is (= ["second" "first" "hero-collection"] (take 3 (map :slug posts))))
    (is (= #{"hero-creator" "hero-line" "explicit-override"} (set (drop 3 (map :slug posts)))))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 4 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (nil? (store/by-slug "no-type")))
  (is (= #{"alpha" "beta" "gamma" "delta"} (store/all-tags))))

(deftest articles-excludes-collections-creators-and-lines
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

(deftest under-line-finds-collections-in-a-line
  (store/load-all! fixture-dir)
  (is (= ["hero-collection"] (map :slug (store/under-line "hero-line"))))
  (is (= [] (store/under-line "no-such-line"))))

(deftest type-is-inferred-from-directory-when-frontmatter-omits-it
  (store/load-all! fixture-dir)
  (is (= :collection (:type (store/by-slug "hero-collection"))))
  (is (= :creator (:type (store/by-slug "hero-creator"))))
  (is (= :line (:type (store/by-slug "hero-line")))))

(deftest explicit-type-overrides-directory-inference
  (store/load-all! fixture-dir)
  (is (= :line (:type (store/by-slug "explicit-override")))))

(deftest type-still-required-outside-inferred-directories
  (store/load-all! fixture-dir)
  (is (nil? (store/by-slug "no-type"))))
