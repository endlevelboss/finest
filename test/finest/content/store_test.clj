(ns finest.content.store-test
  (:require [clojure.test :refer [deftest is]]
            [finest.content.store :as store]))

(def ^:private fixture-dir "test/resources/fixture-posts")

(deftest load-all-skips-malformed-and-sorts-by-date-desc
  (let [posts (store/load-all! fixture-dir)]
    (is (= 16 (count posts)))
    (is (= ["second" "first" "dangling-line-collection" "collection-with-missing-creator"
            "hero-collection" "hero-collection-early"]
           (take 6 (map :slug posts))))
    (is (= #{"hero-creator" "hero-line" "explicit-override" "jane-q-public"
             "fragment-a" "fragment-b" "issue-only-creator" "nested-collection"
             "hero-collection-tba" "about"}
           (set (drop 6 (map :slug posts)))))))

(deftest reviews-carry-the-volumes-they-review
  (store/load-all! fixture-dir)
  (is (= [{:slug "hero-collection" :title (:title (store/by-slug "hero-collection"))}]
         (map #(select-keys % [:slug :title]) (:review-of (store/by-slug "second")))))
  (is (nil? (:review-of (store/by-slug "first")))))

(deftest queries-after-load
  (store/load-all! fixture-dir)
  (is (= "second" (:slug (first (store/by-tag "beta")))))
  (is (= 5 (count (store/by-tag "alpha"))))
  (is (= 1 (count (store/by-type :review))))
  (is (some? (store/by-slug "first")))
  (is (nil? (store/by-slug "broken")))
  (is (nil? (store/by-slug "no-type")))
  (is (= #{"alpha" "beta" "gamma" "delta" "zeta" "eta" "theta"} (store/all-tags))))

(deftest articles-excludes-collections-creators-lines-and-pages
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
  (is (= ["hero-collection-early" "hero-collection" "hero-collection-tba"]
         (map :slug (store/under-line "hero-line"))))
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

(deftest double-dips-of-the-same-issue-resolve-each-other-as-siblings
  ;; fragment-b writes the issue with different case and spacing
  (store/load-all! fixture-dir)
  (let [a-dips (:double-dips (store/by-slug "fragment-a"))
        b-dips (:double-dips (store/by-slug "fragment-b"))]
    (is (= [{:slug "fragment-b" :title "Fragment B" :part "backup"}]
           (:siblings (first a-dips))))
    (is (= [{:slug "fragment-a" :title "Fragment A" :part "lead story"}]
           (:siblings (first b-dips))))
    (is (not (contains? (second a-dips) :siblings)))))

(deftest volumes-without-double-dips-get-no-double-dips-key
  (store/load-all! fixture-dir)
  (is (not (contains? (store/by-slug "hero-collection") :double-dips))))

(deftest releases-around-splits-recent-and-upcoming
  (let [d     #(java.time.LocalDate/parse %)
        vol   (fn [slug date] {:slug slug :release-date (some-> date d)})
        today (d "2026-09-24")
        {:keys [recent upcoming]}
        (store/releases-around [(vol "old" "2026-01-01") (vol "today" "2026-09-24")
                                (vol "last-week" "2026-09-17") (vol "summer" "2026-07-01")
                                (vol "next-week" "2026-10-01") (vol "tba" nil)
                                (vol "far" "2027-06-01") (vol "soon" "2026-09-25")
                                (vol "later" "2026-12-01")]
                               today 3)]
    (is (= ["summer" "last-week" "today"] (map :slug recent)))
    (is (= ["soon" "next-week" "later"] (map :slug upcoming)))))

(deftest drafts-are-left-out-unless-asked-for
  (store/load-all! fixture-dir)
  (is (nil? (store/by-slug "unfinished")))
  (is (not-any? #{"unfinished"} (map :slug (store/referencing "hero-collection")))
      "a draft leaves no trace on the volume it reviews")
  (store/load-all! fixture-dir {:drafts? true})
  (is (:draft (store/by-slug "unfinished")))
  (is (= "unfinished" (:slug (store/review-for "hero-collection"))))
  (store/load-all! fixture-dir))

(defn- write-post!
  [root rel title date]
  (let [f (java.io.File. ^java.io.File root ^String rel)]
    (.mkdirs (.getParentFile f))
    (spit f (str "---\ntitle: \"" title "\"\ndate: " date "\ntype: news\n---\n\nBody.\n"))))

(deftest posts-load-from-year-and-month-subfolders
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "finest-store" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (write-post! root "posts/2026/09/2026-09-01-deep.md" "Deep" "2026-09-01")
    (write-post! root "posts/2026/10/news.md" "October News" "2026-10-01")
    (write-post! root "posts/2026/11/news.md" "November News" "2026-11-01")
    (let [err (java.io.StringWriter.)]
      (binding [*err* err] (store/load-all! (str root)))
      (is (= :news (:type (store/by-slug "deep"))))
      (is (= "posts/2026/09/2026-09-01-deep.md" (:source-path (store/by-slug "deep"))))
      (is (re-find #"Duplicate slug.*:slug news" (str err)))
      (is (re-find #"posts/2026/10/news\.md" (str err)))
      (is (re-find #"posts/2026/11/news\.md" (str err)))
      (is (not (re-find #"deep" (str err)))))
    (store/load-all! fixture-dir)))

(deftest reviews-and-articles-show-their-volumes-tags
  (let [root  (.toFile (java.nio.file.Files/createTempDirectory "finest-tags" (make-array java.nio.file.attribute.FileAttribute 0)))
        write (fn [rel text]
                (let [f (java.io.File. ^java.io.File root ^String rel)]
                  (.mkdirs (.getParentFile f))
                  (spit f text)))]
    (write "collections/vol.md" "---\ntitle: \"Vol\"\ndate: \"1990\"\ntags: [post-crisis]\n---\n\nCollects.\n")
    (write "posts/2026-01-01-rev.md" "---\ntitle: \"Rev\"\ndate: 2026-01-01\ntype: review\ntags: [own]\ncollections: [vol]\n---\n\nBody.\n")
    (write "posts/2026-01-02-news.md" "---\ntitle: \"News\"\ndate: 2026-01-02\ntype: news\ncollections: [vol]\n---\n\nBody.\n")
    (write "posts/2026-01-03-other.md" "---\ntitle: \"Other\"\ndate: 2026-01-03\ntype: news\n---\n\nBody.\n")
    (store/load-all! (str root))
    (is (= #{"own" "post-crisis"} (:tags (store/by-slug "rev"))) "keeps its own tags too")
    (is (= #{"post-crisis"} (:tags (store/by-slug "news"))) "articles about a volume inherit as well")
    (is (= #{} (:tags (store/by-slug "other"))))
    (is (= ["vol"] (map :slug (store/by-tag "post-crisis")))
        "a tag page lists the volume, not the posts that inherit its tag")
    (is (= ["rev"] (map :slug (store/by-tag "own"))) "a post's own tags still list it")
    (is (contains? (store/all-tags) "post-crisis"))
    (store/load-all! fixture-dir)))

(deftest unknown-collection-slugs-are-reported
  (let [root  (.toFile (java.nio.file.Files/createTempDirectory "finest-unknown" (make-array java.nio.file.attribute.FileAttribute 0)))
        write (fn [rel text]
                (let [f (java.io.File. ^java.io.File root ^String rel)]
                  (.mkdirs (.getParentFile f))
                  (spit f text)))]
    (write "collections/vol-a.md" "---\ntitle: \"A\"\ndate: \"1990\"\n---\n\nCollects.\n")
    (write "collections/vol-b.md" "---\ntitle: \"B\"\ndate: \"1991\"\n---\n\nCollects.\n")
    (write "posts/good.md" "---\ntitle: \"Good\"\ndate: 2026-01-01\ntype: review\ncollections: [vol-a, vol-b]\n---\n\nBody.\n")
    (write "posts/no-commas.md" "---\ntitle: \"No commas\"\ndate: 2026-01-02\ntype: review\ncollections: [vol-a vol-b]\n---\n\nBody.\n")
    (write "posts/typo.md" "---\ntitle: \"Typo\"\ndate: 2026-01-03\ntype: review\ncollections: [vol-c]\n---\n\nBody.\n")
    (let [err (java.io.StringWriter.)]
      (binding [*err* err] (store/load-all! (str root)))
      (is (re-find #"missing commas.*posts/no-commas\.md" (str err)))
      (is (re-find #"Unknown collection slug \{:slug vol-c, :file posts/typo\.md\}" (str err)))
      (is (not (re-find #"good\.md" (str err)))))
    (store/load-all! fixture-dir)))
