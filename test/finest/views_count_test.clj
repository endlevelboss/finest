(ns finest.views-count-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [finest.views-count :as vc]))

(def ^:private browser "Mozilla/5.0 (X11; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0")

(defn- fresh-handler
  "A counting handler around a stub that 404s anything but /posts/known
   and /about, with the counts cleared, plus a temp file to flush to."
  []
  (let [f (java.io.File/createTempFile "views" ".edn")]
    (.delete f)
    (reset! @#'vc/counts {})
    (reset! @#'vc/dirty? false)
    [(vc/wrap-count-views
      (fn [{:keys [uri]}]
        {:status (if (#{"/posts/known" "/about"} uri) 200 404) :body ""}))
     f]))

(defn- hit [handler method uri ua]
  (handler {:request-method method :uri uri :headers (if ua {"user-agent" ua} {})}))

(defn- today-counts []
  (get @@#'vc/counts (str (java.time.LocalDate/now))))

(deftest counts-only-real-views-of-posts
  (let [[handler] (fresh-handler)]
    (hit handler :get "/posts/known" browser)
    (hit handler :get "/posts/known" browser)
    (hit handler :head "/posts/known" browser)
    (hit handler :get "/posts/missing" browser)
    (hit handler :get "/about" browser)
    (hit handler :get "/posts/known" "Mozilla/5.0 (compatible; Googlebot/2.1)")
    (hit handler :get "/posts/known" "Mozilla/5.0 (compatible; Discordbot/2.0)")
    (hit handler :get "/posts/known" nil)
    (is (= {"known" 2} (today-counts)))))

(deftest flush-writes-readable-edn-and-only-when-changed
  (let [[handler f] (fresh-handler)]
    (hit handler :get "/posts/known" browser)
    (vc/flush! f)
    (is (= {(str (java.time.LocalDate/now)) {"known" 1}} (edn/read-string (slurp f))))
    (.delete f)
    (vc/flush! f)
    (is (not (.exists f)) "nothing new, so nothing written")))

(deftest top-sums-across-days
  (let [counts {"2026-10-31" {"a" 5 "b" 1}
                "2026-11-01" {"b" 3 "c" 2}
                "2026-11-02" {"b" 1}}]
    (is (= [["a" 5] ["b" 5] ["c" 2]] (sort-by (juxt (comp - second) first) (vc/top counts 3))))
    (is (= [["b" 4] ["c" 2]] (vc/top counts 5 "2026-11-01")))
    (is (= [] (vc/top {} 5)))))
