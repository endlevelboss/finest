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
    (is (some? (r/match-by-path router "/released")))
    (is (some? (r/match-by-path router "/upcoming")))
    (is (some? (r/match-by-path router "/collections")))
    (is (some? (r/match-by-path router "/creators")))
    (is (some? (r/match-by-path router "/lines")))
    (is (some? (r/match-by-path router "/tags")))
    (is (= {:tag "dc"} (:path-params (r/match-by-path router "/tags/dc"))))
    (is (= {:slug "foo-bar"} (:path-params (r/match-by-path router "/posts/foo-bar"))))))

(deftest feed-lists-reviews-and-articles-newest-first
  (let [{:keys [status headers body]} ((routes/app) {:request-method :get :uri "/feed.xml"})
        second-at (.indexOf ^String body "/posts/second</id>")
        first-at  (.indexOf ^String body "/posts/first</id>")]
    (is (= 200 status))
    (is (.startsWith ^String (get headers "Content-Type") "application/atom+xml"))
    (is (< -1 second-at first-at) "newest entry first")
    (is (not (.contains ^String body "/posts/hero-collection</id>")) "volumes stay out of the feed")))

(deftest about-page-is-served-from-content-pages
  (let [{:keys [status body]} ((routes/app) {:request-method :get :uri "/about"})]
    (is (= 200 status))
    (is (.contains ^String body "About this fixture site")))
  (is (not (.contains ^String (:body ((routes/app) {:request-method :get :uri "/"})) "/posts/about"))
      "the about page isn't listed among the posts")
  (is (not (.contains ^String (:body ((routes/app) {:request-method :get :uri "/feed.xml"})) "/posts/about"))
      "nor in the feed"))

(deftest drafts-are-not-served
  (is (= 404 (:status ((routes/app) {:request-method :get :uri "/posts/unfinished"}))))
  (is (not (.contains ^String (:body ((routes/app) {:request-method :get :uri "/feed.xml"})) "unfinished"))))

(deftest drafts-are-badged-when-shown
  (store/load-all! "test/resources/fixture-posts" {:drafts? true})
  (try
    (let [body (:body ((routes/app) {:request-method :get :uri "/posts/unfinished"}))]
      (is (.contains ^String body "type-badge draft")))
    (finally
      (store/load-all! "test/resources/fixture-posts"))))

(deftest sitemap-lists-every-public-page-once
  (let [{:keys [status headers body]} ((routes/app) {:request-method :get :uri "/sitemap.xml"})
        locs (map second (re-seq #"<loc>https?://[^/<]+(/[^<]*)</loc>" body))]
    (is (= 200 status))
    (is (.startsWith ^String (get headers "Content-Type") "application/xml"))
    (is (every? (set locs) ["/" "/reviews" "/about" "/posts/first" "/posts/hero-collection"
                            "/posts/hero-creator" "/tags/alpha"]))
    (is (not-any? #{"/posts/about" "/posts/unfinished"} locs)
        "the about page only under /about, and no drafts")
    (is (= (count locs) (count (set locs))))))

(deftest robots-points-at-the-sitemap
  (let [{:keys [status body]} ((routes/app) {:request-method :get :uri "/robots.txt"})]
    (is (= 200 status))
    (is (.contains ^String body "User-agent: *"))
    (is (re-find #"(?m)^Sitemap: https?://\S+/sitemap\.xml$" body))))

(deftest head-requests-are-answered-like-get
  (doseq [uri ["/" "/posts/first" "/feed.xml" "/css/style.css"]]
    (let [{:keys [status body]} ((routes/app) {:request-method :head :uri uri})]
      (is (= 200 status) uri)
      (is (nil? body) uri))))

(deftest static-files-are-cached-only-when-asked
  (let [css {:request-method :get :uri "/css/style.css"}]
    (is (nil? (get-in ((routes/app) css) [:headers "Cache-Control"])))
    (is (= "public, max-age=86400"
           (get-in ((routes/app {:cache-static? true}) css) [:headers "Cache-Control"])))
    (is (nil? (get-in ((routes/app {:cache-static? true}) {:request-method :get :uri "/"})
                      [:headers "Cache-Control"]))
        "pages themselves aren't cached")))

(deftest unchanged-static-files-get-a-304
  (let [app           (routes/app {:cache-static? true})
        last-modified (get-in (app {:request-method :get :uri "/css/style.css"}) [:headers "Last-Modified"])]
    (is (some? last-modified))
    (is (= 304 (:status (app {:request-method :get :uri "/css/style.css"
                              :headers {"if-modified-since" last-modified}}))))))

(defn- get-page [uri query]
  ((routes/app) {:request-method :get :uri uri :query-string query}))

(deftest listings-are-paged
  (with-redefs [finest.handler/page-size 1]
    (let [page-1 (get-page "/" nil)
          page-2 (get-page "/" "page=2")]
      (is (= 200 (:status page-1)))
      (is (.contains ^String (:body page-1) "/posts/second") "newest first")
      (is (not (.contains ^String (:body page-1) "/posts/first\"")))
      (is (.contains ^String (:body page-1) "href=\"/?page=2\""))
      (is (not (.contains ^String (:body page-1) "Newer")) "no Newer link on page 1")
      (is (.contains ^String (:body page-2) "/posts/first\""))
      (is (.contains ^String (:body page-2) "href=\"/\" rel=\"prev\"") "page 1 is the plain address")
      (is (.contains ^String (:body page-2) "Page 2 of 2"))
      (is (not (.contains ^String (:body page-2) "Older")) "no Older link on the last page")
      (is (.contains ^String (:body page-2) "<title>Latest, page 2")))
    (is (.contains ^String (:body (get-page "/tags/alpha" "page=2")) "href=\"/tags/alpha?page=3\""))))

(deftest bad-page-numbers-are-not-found
  (doseq [q ["page=99" "page=0" "page=-1" "page=abc" "page=" "page=1.5"]]
    (is (= 404 (:status (get-page "/reviews" q))) q))
  (is (= 200 (:status (get-page "/reviews" "page=1"))))
  (is (= 200 (:status (get-page "/reviews" "utm_source=x"))) "other parameters are ignored"))

(deftest short-listings-have-no-pager
  (is (not (.contains ^String (:body (get-page "/" nil)) "class=\"pager\""))))

(defn- meta-content [body attr-name]
  (second (re-find (re-pattern (str "<meta content=\"([^\"]*)\"[^>]*(?:name|property)=\"" attr-name "\"")) body)))

(deftest a-review-describes-itself-for-previews
  (let [body (:body (get-page "/posts/second" nil))]
    (is (= "Second body." (meta-content body "description")))
    (is (= "Second Post" (meta-content body "og:title")))
    (is (= "article" (meta-content body "og:type")))
    (is (re-find #"^https?://[^/]+/posts/second$" (meta-content body "og:url")))
    (is (re-find #"^https?://[^/]+/images/hero-cover\.jpg$" (meta-content body "og:image"))
        "falls back to the reviewed volume's cover")))

(deftest listings-use-the-tagline-and-banner
  (let [body (:body (get-page "/" nil))]
    (is (= finest.views.components/site-tagline (meta-content body "description")))
    (is (= "website" (meta-content body "og:type")))
    (is (re-find #"/images/banner\.png$" (meta-content body "og:image"))))
  (with-redefs [finest.handler/page-size 1]
    (is (re-find #"/\?page=2$" (meta-content (:body (get-page "/" "page=2")) "og:url")))))

(deftest pages-advertise-the-feed
  (let [body (:body ((routes/app) {:request-method :get :uri "/"}))]
    (is (.contains ^String body "type=\"application/atom+xml\""))))

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
           (map (fn [[heading vols]] [heading (map :slug vols)]) groups)))))

(deftest volumes-split-into-upcoming-and-released
  (let [d     #(java.time.LocalDate/parse %)
        split (#'finest.handler/split-releases
               [{:slug "old" :release-date (d "2025-11-04")} {:slug "tba"}
                {:slug "today" :release-date (d "2026-10-28")} {:slug "far" :release-date (d "2027-03-01")}
                {:slug "soon" :release-date (d "2026-11-10")}]
               (d "2026-10-28"))]
    (is (= ["soon" "far" "tba"] (map :slug (:upcoming split))) "soonest first, undated last")
    (is (= ["today" "old"] (map :slug (:released split))) "today counts as out; newest first")))

(deftest released-and-upcoming-are-separate-pages
  (let [app      (routes/app)
        get-page #(app {:request-method :get :uri %})]
    (is (= 200 (:status (get-page "/released"))))
    (is (= 200 (:status (get-page "/upcoming"))))
    (is (.contains ^String (:body (get-page "/upcoming")) "href=\"/posts/hero-collection\"")
        "undated volumes count as upcoming")
    (is (not (.contains ^String (:body (get-page "/released")) "href=\"/posts/hero-collection\"")))
    (is (= {:status 301 :location "/released"}
           (let [r (get-page "/collections")] {:status (:status r) :location (get-in r [:headers "Location"])})))
    (is (.contains ^String (:body (get-page "/")) "href=\"/upcoming\"") "both are in the menu")
    (is (.contains ^String (:body (get-page "/")) "href=\"/released\""))))

(deftest volumes-page-lists-newest-first
  (let [d    #(java.time.LocalDate/parse %)
        vols (sort @#'finest.handler/by-release-date-desc
                   [{:slug "tba"} {:slug "a" :release-date (d "2025-11-04")}
                    {:slug "c" :release-date (d "2026-09-01")} {:slug "b" :release-date (d "2026-01-13")}])]
    (is (= ["c" "b" "a" "tba"] (map :slug vols)))))

(deftest volumes-listing-links-each-volume-to-its-line
  (let [body (:body ((routes/app) {:request-method :get :uri "/upcoming"}))]
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
