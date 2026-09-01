(ns finest.content.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [finest.content.post :as post])
  (:import [java.time LocalDate]))

(deftest slug-from-filename-test
  (is (= "watchmen-annotated-review"
         (post/slug-from-filename "2026-01-15-watchmen-annotated-review.md")))
  (is (= "no-date-prefix"
         (post/slug-from-filename "no-date-prefix.md"))))

(defn- base-meta
  []
  {:title "Some Title" :date "2026-01-15" :type "news"})

(deftest builds-valid-news-post
  (let [p (post/->post {:meta (base-meta) :html "<p>hi</p>"
                         :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= "x" (:slug p)))
    (is (= :news (:type p)))
    (is (= (LocalDate/parse "2026-01-15") (:date p)))
    (is (not (contains? p :rating)))))

(deftest builds-valid-review-post-with-rating
  (let [p (post/->post {:meta (assoc (base-meta) :type "review" :rating 4.5)
                         :html "<p>hi</p>" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= :review (:type p)))
    (is (= 4.5 (:rating p)))))

(deftest rejects-missing-title
  (is (thrown? clojure.lang.ExceptionInfo
               (post/->post {:meta (dissoc (base-meta) :title) :html ""
                              :source-file "x.md" :last-modified 0}))))

(deftest rejects-unknown-type
  (is (thrown? clojure.lang.ExceptionInfo
               (post/->post {:meta (assoc (base-meta) :type "editorial") :html ""
                              :source-file "x.md" :last-modified 0}))))

(deftest rejects-review-without-rating
  (testing "reviews must include a rating"
    (is (thrown? clojure.lang.ExceptionInfo
                 (post/->post {:meta (assoc (base-meta) :type "review") :html ""
                                :source-file "x.md" :last-modified 0})))))

(deftest explicit-slug-overrides-filename
  (let [p (post/->post {:meta (assoc (base-meta) :slug "custom-slug") :html ""
                         :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= "custom-slug" (:slug p)))))

(deftest cover-field-is-optional
  (let [without-cover (post/->post {:meta (base-meta) :html ""
                                     :source-file "2026-01-15-x.md" :last-modified 0})
        with-cover    (post/->post {:meta (assoc (base-meta) :cover "/images/x.jpg") :html ""
                                     :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? without-cover :cover)))
    (is (= "/images/x.jpg" (:cover with-cover)))))
