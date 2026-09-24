(ns finest.content.post-test
  (:require [clojure.test :refer [deftest is testing]]
            [finest.content.post :as post])
  (:import [java.time LocalDate]))

(deftest slug-from-filename-test
  (is (= "watchmen-annotated-review"
         (post/slug-from-filename "2026-01-15-watchmen-annotated-review.md")))
  (is (= "no-date-prefix"
         (post/slug-from-filename "no-date-prefix.md"))))

(deftest humanize-slug-test
  (is (= "Bill Finger" (post/humanize-slug "bill-finger")))
  (is (= "Batman" (post/humanize-slug "batman"))))

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

(deftest builds-valid-collection-post-without-rating
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection")
                         :html "<p>hi</p>" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= :collection (:type p)))
    (is (not (contains? p :rating)))))

(deftest collections-field-parses-into-vector
  (let [without (post/->post {:meta (base-meta) :html ""
                               :source-file "2026-01-15-x.md" :last-modified 0})
        with    (post/->post {:meta (assoc (base-meta) :collections ["a" "b"]) :html ""
                               :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? without :collections)))
    (is (= ["a" "b"] (:collections with)))))

(deftest collection-date-is-freeform-with-year-extracted-for-sorting
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection" :date "Sep 1986 - Oct 1987")
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= "Sep 1986 - Oct 1987" (:date-display p)))
    (is (= (LocalDate/parse "1986-01-01") (:date p)))))

(deftest collection-date-without-a-year-has-no-sort-date
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection" :date "ongoing")
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= "ongoing" (:date-display p)))
    (is (not (contains? p :date)))))

(deftest line-field-is-optional
  (let [without (post/->post {:meta (assoc (base-meta) :type "collection") :html ""
                               :source-file "2026-01-15-x.md" :last-modified 0})
        with    (post/->post {:meta (assoc (base-meta) :type "collection" :line "batman") :html ""
                               :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? without :line)))
    (is (= "batman" (:line with)))))

(deftest builds-valid-line-post-without-date-or-rating
  (let [p (post/->post {:meta {:title "Some Franchise" :type "line"} :html "<p>bio</p>"
                         :source-file "some-franchise.md" :last-modified 0})]
    (is (= :line (:type p)))
    (is (not (contains? p :date)))
    (is (not (contains? p :rating)))))

(deftest builds-valid-creator-post-without-date
  (let [p (post/->post {:meta {:title "Some Creator" :type "creator"} :html "<p>bio</p>"
                         :source-file "some-creator.md" :last-modified 0})]
    (is (= :creator (:type p)))
    (is (not (contains? p :date)))))

(deftest creators-field-parses-into-vector-of-maps
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection"
                                      :creators [{:slug "a" :role "Writer"}])
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= [{:slug "a" :role "Writer"}] (:creators p)))))

(deftest creators-role-is-optional-on-a-map-entry
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection"
                                      :creators [{:slug "a"}])
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= [{:slug "a"}] (:creators p)))))

(deftest creators-field-accepts-bare-slug-strings
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection"
                                      :creators ["a" "b"])
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= [{:slug "a"} {:slug "b"}] (:creators p)))))

(deftest issues-field-parses-only-present-fields
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection"
                                      :issues [{:number 404}
                                               {:number 405 :date "Mar 1987" :original "action-comics-256"
                                                :creators [{:slug "a" :role "Writer"} "b"]}])
                         :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (= [{:number 404}
            {:number 405 :date "Mar 1987" :original "action-comics-256"
             :creators [{:slug "a" :role "Writer"} {:slug "b"}]}]
           (:issues p)))))

(deftest issues-field-is-optional
  (let [p (post/->post {:meta (assoc (base-meta) :type "collection") :html ""
                         :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? p :issues)))))

(deftest release-date-is-optional-and-formatted-for-display
  (let [without (post/->post {:meta (assoc (base-meta) :type "collection") :html ""
                               :source-file "2026-01-15-x.md" :last-modified 0})
        with    (post/->post {:meta (assoc (base-meta) :type "collection" :release-date "2024-11-05")
                               :html "" :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? without :release-date-display)))
    (is (not (contains? without :release-date)))
    (is (= "Nov 5, 2024" (:release-date-display with)))
    (is (= (LocalDate/parse "2024-11-05") (:release-date with)))))

(deftest upcoming-is-judged-against-the-given-day
  (let [vol {:release-date (LocalDate/parse "2026-10-06")}]
    (is (true?  (post/upcoming? vol (LocalDate/parse "2026-10-05"))))
    (is (false? (post/upcoming? vol (LocalDate/parse "2026-10-06"))) "out on release day")
    (is (false? (post/upcoming? vol (LocalDate/parse "2026-10-07"))))
    (is (false? (post/upcoming? {} (LocalDate/parse "2026-10-05"))) "no date, not upcoming")))

(deftest cover-field-is-optional
  (let [without-cover (post/->post {:meta (base-meta) :html ""
                                     :source-file "2026-01-15-x.md" :last-modified 0})
        with-cover    (post/->post {:meta (assoc (base-meta) :cover "/images/x.jpg") :html ""
                                     :source-file "2026-01-15-x.md" :last-modified 0})]
    (is (not (contains? without-cover :cover)))
    (is (= "/images/x.jpg" (:cover with-cover)))))
