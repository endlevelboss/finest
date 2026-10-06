(ns finest.routes-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [reitit.core :as r]
            [finest.content.store :as store]
            [finest.routes :as routes]))

(use-fixtures :once
  (fn [run-tests]
    (store/load-all! "test/resources/fixture-posts")
    (run-tests)))

(deftest route-resolution
  (let [router (routes/router)]
    (is (some? (r/match-by-path router "/")))
    (is (nil? (r/match-by-path router "/news")) "the front page lists everything")
    (is (some? (r/match-by-path router "/reviews")))
    (is (some? (r/match-by-path router "/articles")))
    (is (some? (r/match-by-path router "/collections")))
    (is (some? (r/match-by-path router "/creators")))
    (is (some? (r/match-by-path router "/lines")))
    (is (some? (r/match-by-path router "/tags")))
    (is (= {:tag "dc"} (:path-params (r/match-by-path router "/tags/dc"))))
    (is (= {:slug "foo-bar"} (:path-params (r/match-by-path router "/posts/foo-bar"))))))

(deftest app-returns-200-for-index
  (let [response ((routes/app) {:request-method :get :uri "/"})]
    (is (= 200 (:status response)))))

(deftest app-returns-200-for-known-slug
  (let [response ((routes/app) {:request-method :get :uri "/posts/first"})]
    (is (= 200 (:status response)))))

(deftest app-returns-404-for-unknown-slug
  (let [response ((routes/app) {:request-method :get :uri "/posts/does-not-exist"})]
    (is (= 404 (:status response)))))

(deftest app-returns-404-for-unknown-path
  (let [response ((routes/app) {:request-method :get :uri "/nope"})]
    (is (= 404 (:status response)))))

(deftest reviews-and-articles-list-only-their-own-type
  (let [body #(:body ((routes/app) {:request-method :get :uri %}))
        listed? (fn [page slug] (.contains ^String (body page) (str "href=\"/posts/" slug "\"")))]
    (is (listed? "/reviews" "second"))
    (is (not (listed? "/reviews" "first")))
    (is (listed? "/articles" "first"))
    (is (not (listed? "/articles" "second")))))

(deftest review-page-shows-what-it-reviews
  (let [body (:body ((routes/app) {:request-method :get :uri "/posts/second"}))]
    (is (.contains ^String body "A review of: "))
    (is (.contains ^String body "href=\"/posts/hero-collection\""))
    (is (.contains ^String body "href=\"/posts/hero-line\""))
    (is (not (.contains ^String body "About: ")))))

(deftest volumes-page-groups-by-release-year
  (let [d      #(java.time.LocalDate/parse %)
        groups (#'finest.handler/release-year-groups
                [{:slug "a" :release-date (d "2025-11-04")} {:slug "b" :release-date (d "2026-01-13")}
                 {:slug "c" :release-date (d "2026-09-01")} {:slug "tba"}])]
    (is (= [["2025" ["a"]] ["2026" ["b" "c"]] ["No release date" ["tba"]]]
           (map (fn [[heading vols]] [heading (map :slug vols)]) groups))))
  (let [body (:body ((routes/app) {:request-method :get :uri "/collections"}))]
    (is (.contains ^String body "listing-group-heading"))))

(deftest volumes-listing-links-each-volume-to-its-line
  (let [body (:body ((routes/app) {:request-method :get :uri "/collections"}))]
    (is (.contains ^String body "href=\"/posts/hero-line\""))
    (is (.contains ^String body "href=\"/posts/hero-collection\"")))
  (let [body (:body ((routes/app) {:request-method :get :uri "/posts/hero-line"}))]
    (is (.contains ^String body "href=\"/posts/hero-collection\""))
    (is (not (.contains ^String body "title-sep")))))

(deftest volume-page-lists-its-double-dips
  (let [body (:body ((routes/app) {:request-method :get :uri "/posts/fragment-a"}))]
    (is (.contains ^String body "Double-dips"))
    (is (.contains ^String body "Shared Comics #256"))
    (is (.contains ^String body "href=\"/posts/fragment-b\""))
    (is (.contains ^String body "(backup)")))
  (let [body (:body ((routes/app) {:request-method :get :uri "/posts/hero-collection"}))]
    (is (not (.contains ^String body "Double-dips")))))
