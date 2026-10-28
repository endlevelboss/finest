(ns finest.handler
  (:require [clojure.string :as str]
            [finest.content.post :as post]
            [finest.content.store :as store]
            [finest.views.layout :as layout]
            [finest.views.index :as index-view]
            [finest.views.post :as post-view]
            [finest.views.embeds :as embeds]
            [finest.views.feed :as feed-view]
            [finest.views.sitemap :as sitemap-view]
            [finest.views.components :as components]
            [finest.views.meta :as meta])
  (:import (java.time LocalDate)))

(defn- html-response
  [status body]
  {:status  status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})

(defn- site-url
  "The site's public base URL, from SITE_URL, without a trailing slash.
   Falls back to the dev server until the site has a real address."
  []
  (str/replace (or (System/getenv "SITE_URL") "http://localhost:3000") #"/+$" ""))

(defn- page-number
  "The ?page= asked for: 1 when there's none, nil when it isn't a
   positive whole number."
  [{:keys [query-string]}]
  (let [raw (second (re-find #"(?:^|&)page=([^&]*)" (or query-string "")))]
    (cond
      (nil? raw)                       1
      (re-matches #"[1-9]\d{0,5}" raw) (parse-long raw))))

(def ^:private fallback-image "/images/banner.png")

(defn- page
  "Every page carries the release sidebar, judged against today's date,
   and meta tags for search results and link previews: the site's
   tagline and banner, unless `meta` has something better for this page."
  [request {:keys [title meta] :as opts}]
  (let [base  (site-url)
        n     (page-number request)
        tags  (merge {:title       (or title components/site-name)
                      :description components/site-tagline
                      :image       fallback-image
                      :type        "website"
                      :url         (str base (:uri request) (when (and n (> n 1)) (str "?page=" n)))}
                     meta)]
    (layout/page (assoc opts
                        :meta    (update tags :image #(meta/absolute-url base %))
                        :sidebar (store/release-window (LocalDate/now) 3)))))

(defn- listing-response
  ([request heading posts] (listing-response request heading posts nil nil))
  ([request heading posts card list-class]
   (html-response 200 (page request {:title heading
                                    :body  (index-view/listing-page
                                             (cond-> {:heading heading :posts posts}
                                               card       (assoc :card card)
                                               list-class (assoc :list-class list-class)))}))))

(def ^:private page-size 10)

(defn- paginate
  "One page of `posts` plus what the pager links need, or nil for a
   page number past the end."
  [request posts]
  (let [pages (max 1 (quot (+ (count posts) (dec page-size)) page-size))
        n     (page-number request)]
    (when (and n (<= n pages))
      {:posts (into [] (comp (drop (* (dec n) page-size)) (take page-size)) posts)
       :pager {:page n :pages pages :path (:uri request)}})))

(declare not-found)

(defn- paged-listing-response
  "A listing that grows with every post, split into pages. `title` is
   the browser-tab title (nil for the site name alone); later pages say
   which page they are."
  [request {:keys [title heading posts]}]
  (if-let [{:keys [posts pager]} (paginate request posts)]
    (html-response 200 (page request {:title (if (> (:page pager) 1)
                                               (str (or title heading) ", page " (:page pager))
                                               title)
                                      :body  (index-view/listing-page
                                               {:heading heading :posts posts :pager pager})}))
    (not-found request)))

(defn index
  [request]
  (paged-listing-response request {:heading "Latest" :posts (store/articles)}))

(defn reviews-list
  [request]
  (paged-listing-response request {:title "Reviews" :heading "Reviews" :posts (store/by-type :review)}))

(defn articles-list
  "Every non-review writeup -- the :news type, badged \"Article\"."
  [request]
  (paged-listing-response request {:title "Articles" :heading "Articles" :posts (store/by-type :news)}))

(defn- undated-last
  "A release-date comparator from `cmp`, with volumes that have no known
   release date (nothing announced yet) after every dated one."
  [cmp]
  (fn [a b]
    (let [ra (:release-date a)
          rb (:release-date b)]
      (cond
        (and ra rb) (cmp ra rb)
        ra          -1
        rb          1
        :else       0))))

(def ^:private by-release-date-desc
  "Newest release first, undated last."
  (undated-last #(compare %2 %1)))

(def ^:private by-release-date-asc
  "Soonest release first, undated last."
  (undated-last compare))

(defn- attach-review
  "The review (if any) of this collection, for volume-row's third column."
  [collection]
  (if-let [review (store/review-for (:slug collection))]
    (assoc collection :review review)
    collection))

(defn- release-year-groups
  "Volumes already sorted by release date, split into [year volumes]
   groups -- undated volumes last, under their own heading."
  [volumes]
  (for [group (partition-by #(some-> (:release-date %) .getYear) volumes)]
    [(if-let [d (:release-date (first group))] (str (.getYear ^LocalDate d)) "No release date")
     group]))

(defn- split-releases
  "Volumes split around `today`, which counts as out: :upcoming soonest
   first, with not-yet-dated volumes last, and :released newest first."
  [volumes today]
  (let [upcoming? #(or (nil? (:release-date %)) (post/upcoming? % today))]
    {:upcoming (sort by-release-date-asc (filter upcoming? volumes))
     :released (sort by-release-date-desc (remove upcoming? volumes))}))

(defn- volumes
  [side]
  (side (split-releases (mapv attach-review (store/by-type :collection)) (LocalDate/now))))

(defn released-list
  "Volumes already out, under their release years."
  [request]
  (html-response 200 (page request {:title "Released"
                                    :body  (index-view/grouped-listing-page
                                             {:heading "Released"
                                              :groups  (release-year-groups (volumes :released))
                                              :card    components/volume-row})})))

(defn upcoming-list
  "Volumes still to come, next one first."
  [request]
  (listing-response request "Upcoming" (volumes :upcoming) components/volume-row nil))

(defn collections-redirect
  "The old combined Volumes page, kept so links to it still land somewhere."
  [_request]
  {:status 301 :headers {"Location" "/released"} :body ""})

(def ^:private feed-size 20)

(defn feed
  "The Atom feed: the newest reviews and articles, as on the front page."
  [_request]
  {:status  200
   :headers {"Content-Type" "application/atom+xml; charset=utf-8"}
   :body    (feed-view/feed
              {:base-url (site-url)
               :posts    (map #(update % :html embeds/expand store/by-slug)
                              (take feed-size (store/articles)))})})

(def ^:private listing-paths
  ["/" "/reviews" "/articles" "/released" "/upcoming" "/creators" "/lines" "/tags"])

(defn sitemap
  "Every public page: the listings, each post (pages have their own
   addresses, like /about), and each tag."
  [_request]
  {:status  200
   :headers {"Content-Type" "application/xml; charset=utf-8"}
   :body    (sitemap-view/sitemap
              {:base-url (site-url)
               :paths    (concat listing-paths
                                 (when (= :page (:type (store/by-slug "about"))) ["/about"])
                                 (for [{:keys [slug type]} (store/all-posts)
                                       :when (not= :page type)]
                                   (str "/posts/" slug))
                                 (for [tag (store/all-tags)]
                                   (str "/tags/" tag)))})})

(defn robots
  "Every crawler is welcome; drafts need no rule, since they're never served."
  [_request]
  {:status  200
   :headers {"Content-Type" "text/plain; charset=utf-8"}
   :body    (str "User-agent: *\n"
                 "Allow: /\n"
                 "\n"
                 "Sitemap: " (site-url) "/sitemap.xml\n")})

(defn creators-list
  [request]
  (listing-response request "Creators" (store/by-type :creator)))

(defn- title-sort-key
  "Alphabetizes on a title while ignoring a leading \"The \", so \"The
   Flash\" sorts under F rather than T."
  [{:keys [title]}]
  (-> title (str/replace #"(?i)^the\s+" "") str/lower-case))

(defn lines-list
  [request]
  (listing-response request "Lines" (sort-by title-sort-key (store/by-type :line)) components/line-row "line-columns"))

(defn tag-index
  [request]
  (let [tags (store/all-tags)]
    (html-response 200
      (page request
        {:title "Tags"
         :body  [:section.listing
                 [:h1 "Tags"]
                 [:ul.tag-index
                  (for [tag tags]
                    [:li [:a {:href (str "/tags/" tag)} tag]])]]}))))

(defn tag-list
  [request]
  (let [tag     (get-in request [:path-params :tag])
        heading (str "Tag: " tag)]
    (paged-listing-response request {:title heading :heading heading :posts (store/by-tag tag)})))

(defn- creator-role-on
  [collection creator-slug]
  (some #(when (= creator-slug (:slug %)) (:role %)) (:creators collection)))

(defn- resolve-creators
  [creators]
  (keep (fn [{:keys [slug role]}] (some-> (store/by-slug slug) (assoc :role role))) creators))

(defn- resolve-double-dip-creators
  [dips]
  (map (fn [dip]
         (cond-> dip
           (seq (:creators dip)) (assoc :creators (resolve-creators (:creators dip)))))
       dips))

(defn- enrich
  [{:keys [type slug collections creators double-dips] :as post}]
  (cond-> (update post :html embeds/expand store/by-slug)
    (seq collections)     (assoc :referenced-collections (keep store/by-slug collections))
    (= type :collection)  (assoc :related (store/referencing slug)
                                  :creator-credits (resolve-creators creators))
    (seq double-dips)     (assoc :double-dips (resolve-double-dip-creators double-dips))
    (= type :creator)     (assoc :credited-collections
                                  (map #(assoc % :role (creator-role-on % slug)) (store/credited-on slug)))
    (= type :line)        (assoc :line-collections (mapv attach-review (store/under-line slug)))))

(defn- post-meta
  "A post's own description and preview image, where it has them: a
   written `description:`, else its first paragraph; its featured image,
   else its cover, else the cover of a volume it's about."
  [{:keys [type title description html feature-image cover collections] :as post}]
  (let [text  (or description (meta/excerpt html))
        image (or (:src feature-image)
                  cover
                  (some #(:cover (store/by-slug %)) collections))]
    (cond-> {:title (components/display-title post)
             :type  (if (#{:news :review} type) "article" "website")}
      text  (assoc :description text)
      image (assoc :image image))))

(defn- post-response
  [request post]
  (let [enriched (enrich post)]
    (html-response 200 (page request {:title (components/display-title enriched)
                                      :meta  (post-meta enriched)
                                      :body  (post-view/post-page enriched)}))))

(defn post-page
  [request]
  (let [slug (get-in request [:path-params :slug])]
    (if-let [post (store/by-slug slug)]
      (post-response request post)
      (html-response 404 (page request {:title "Not Found" :body [:p "Post not found."]})))))

(defn not-found
  [request]
  (html-response 404 (page request {:title "Not Found" :body [:p "Page not found."]})))

(defn about
  "The about page: content/pages/about.md."
  [request]
  (let [post (store/by-slug "about")]
    (if (= :page (:type post))
      (post-response request post)
      (not-found request))))
