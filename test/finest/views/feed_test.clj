(ns finest.views.feed-test
  (:require [clojure.test :refer [deftest is]]
            [finest.views.feed :as feed])
  (:import (java.time LocalDate)))

(deftest absolutize-links-rewrites-root-relative-urls-only
  (is (= "<a href=\"https://x.test/posts/a\"><img src=\"https://x.test/images/b.jpg\"></a>"
         (feed/absolutize-links "<a href=\"/posts/a\"><img src=\"/images/b.jpg\"></a>" "https://x.test")))
  (is (= "<a href=\"https://elsewhere.test/\">x</a><img src=\"//cdn.test/c.png\">"
         (feed/absolutize-links "<a href=\"https://elsewhere.test/\">x</a><img src=\"//cdn.test/c.png\">"
                                "https://x.test"))))

(deftest feed-escapes-titles-and-bodies
  (let [xml (feed/feed {:base-url "https://x.test"
                        :posts    [{:slug "tj" :title "Tom & Jerry" :date (LocalDate/parse "2026-03-04")
                                    :tags #{"b" "a"} :html "<p>Hi</p>"}]})]
    (is (.startsWith ^String xml "<?xml"))
    (is (.contains ^String xml "<title>Tom &amp; Jerry</title>"))
    (is (.contains ^String xml "&lt;p&gt;Hi&lt;/p&gt;"))
    (is (.contains ^String xml "<id>https://x.test/posts/tj</id>"))
    (is (.contains ^String xml "<updated>2026-03-04T00:00:00Z</updated>"))))
