(ns finest.views.meta-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [finest.views.meta :as meta]))

(deftest excerpt-takes-the-first-paragraph-with-text
  (is (= "Tom & Jerry, again."
         (meta/excerpt (str "<p><img src=\"/x.jpg\" alt=\"x\"></p>"
                            "<p>Tom &amp; <em>Jerry</em>,\n again.</p><p>Second.</p>"))))
  (is (= "HJ's ring 'n' \"stuff\" &copy;"
         (meta/excerpt "<p>HJ&apos;s ring &#39;n&#x27; &quot;stuff&quot; &copy;</p>")))
  (is (nil? (meta/excerpt "<h2>Only a heading</h2>")))
  (is (nil? (meta/excerpt nil))))

(deftest excerpt-is-cut-at-a-word-boundary
  (let [long-text (str/join " " (repeat 60 "word"))
        out       (meta/excerpt (str "<p>" long-text "</p>"))]
    (is (<= (count out) 156))
    (is (str/ends-with? out "word…"))))

(deftest absolute-url-only-touches-root-relative-paths
  (is (= "https://x.test/images/a.jpg" (meta/absolute-url "https://x.test" "/images/a.jpg")))
  (is (= "https://cdn.test/a.jpg" (meta/absolute-url "https://x.test" "https://cdn.test/a.jpg")))
  (is (= "//cdn.test/a.jpg" (meta/absolute-url "https://x.test" "//cdn.test/a.jpg"))))
