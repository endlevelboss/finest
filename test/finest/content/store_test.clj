(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 14 (count posts)))
    (is (= ["second" "first" "dangling-line-collection" "collection-with-missing-creator"
            "hero-collection" "hero-collection-early"]
           (take 6 (map :slug posts))))
    (is (= #{"hero-creator" "hero-line" "explicit-override" "jane-q-public"
             "fragment-a" "fragment-b" "issue-only-creator" "nested-collection"}
           (set (drop 6 (map :slug posts)))))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 5 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (nil? (store/by-slug "no-type")))
  (is (= #{"alpha" "beta" "gamma" "delta" "zeta" "eta" "theta"} (store/all-tags))))

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

(deftest under-line-finds-collections-in-a-line-oldest-first
  (store/load-all! fixture-dir)
  (is (= ["hero-collection-early" "hero-collection"] (map :slug (store/under-line "hero-line"))))
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

(deftest type-is-inferred-through-nested-subfolders
  (store/load-all! fixture-dir)
  (is (= :collection (:type (store/by-slug "nested-collection")))))

(deftest collection-gets-line-title-attached
  (store/load-all! fixture-dir)
  (is (= "Hero Line" (:line-title (store/by-slug "hero-collection")))))

(deftest collection-without-a-line-has-no-line-title
  (store/load-all! fixture-dir)
  (is (not (contains? (store/by-slug "first") :line-title))))

(deftest collection-with-dangling-line-has-no-line-title
  (store/load-all! fixture-dir)
  (is (not (contains? (store/by-slug "dangling-line-collection") :line-title))))

(deftest missing-creator-gets-an-auto-generated-page
  (store/load-all! fixture-dir)
  (let [p (store/by-slug "jane-q-public")]
    (is (= :creator (:type p)))
    (is (= "Jane Q Public" (:title p)))
    (is (true? (:generated? p)))))

(deftest auto-generated-creator-lists-their-collections
  (store/load-all! fixture-dir)
  (is (= ["collection-with-missing-creator"] (map :slug (store/credited-on "jane-q-public")))))

(deftest real-creator-file-is-not-treated-as-generated
  (store/load-all! fixture-dir)
  (is (not (contains? (store/by-slug "hero-creator") :generated?))))

(deftest credited-on-finds-issue-only-creators
  (store/load-all! fixture-dir)
  (is (= ["fragment-a"] (map :slug (store/credited-on "issue-only-creator")))))

(deftest issue-only-creator-gets-an-auto-generated-page
  (store/load-all! fixture-dir)
  (is (= "Issue Only Creator" (:title (store/by-slug "issue-only-creator")))))

(deftest issues-sharing-an-original-id-resolve-each-other-as-siblings
  (store/load-all! fixture-dir)
  (let [a-issues (:issues (store/by-slug "fragment-a"))
        b-issues (:issues (store/by-slug "fragment-b"))]
    (is (= [{:slug "fragment-b" :title "Fragment B" :number 256}]
           (:siblings (first a-issues))))
    (is (= [{:slug "fragment-a" :title "Fragment A" :number 256}]
           (:siblings (first b-issues))))
    (is (not (contains? (second a-issues) :siblings)))))
