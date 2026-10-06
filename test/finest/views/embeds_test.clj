(ns finest.views.embeds-test
  (:require [clojure.test :refer [deftest is]]
            [finest.views.embeds :as embeds]))

(def ^:private posts
  {"some-review"  {:slug "some-review" :type :review :title "Some Review" :rating 3.0
                   :review-of [{:slug "vol" :title "Year One" :line-title "Batman"}]
                   :feature-image {:src "/images/x.jpg" :alt "X"}}
   "some-article" {:slug "some-article" :type :news :title "Some Article"}
   "odd-title"    {:slug "odd-title" :type :news :title "Tom & \"Jerry\""}})

(defn- expand [html] (embeds/expand html posts))

(deftest inline-review-link-gets-title-and-stars
  (let [out (expand "<p>See [[some-review]] now.</p>")]
    (is (re-find #"<a href=\"/posts/some-review\">Some Review</a>" out))
    (is (re-find #"class=\"rating\"" out))
    (is (not (re-find #"\[\[" out)))))

(deftest inline-non-review-link-has-no-stars
  (let [out (expand "<p>See [[some-article]].</p>")]
    (is (re-find #"<a href=\"/posts/some-article\">Some Article</a>" out))
    (is (not (re-find #"rating" out)))))

(deftest unknown-slugs-are-left-alone
  (is (= "<p>[[nope]]</p><p>{{review nope}}</p>"
         (expand "<p>[[nope]]</p><p>{{review nope}}</p>"))))

(deftest review-card-replaces-its-paragraph
  (let [out (expand "<p>Intro.</p><p>{{review some-review}}</p>")]
    (is (re-find #"^<p>Intro.</p><a class=\"review-embed\" href=\"/posts/some-review\">" out))
    (is (re-find #"A review of: Batman: Year One" out))
    (is (re-find #"src=\"/images/x.jpg\"" out))
    (is (not (re-find #"\{\{" out)))))

(deftest review-card-needs-a-review
  (is (= "<p>{{review some-article}}</p>" (expand "<p>{{review some-article}}</p>"))))

(deftest titles-are-escaped
  (let [out (expand "<p>[[odd-title]]</p>")]
    (is (re-find #"Tom &amp; &quot;Jerry&quot;" out))))
