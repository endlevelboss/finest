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
    (is (some? (r/match-by-path router "/news")))
    (is (some? (r/match-by-path router "/reviews")))
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
