(ns finest.handler
  (:require [clojure.string :as str]
            [finest.content.store :as store]
            [finest.views.layout :as layout]
            [finest.views.index :as index-view]
            [finest.views.post :as post-view]
            [finest.views.components :as components]))

(defn- html-response
  [status body]
  {:status  status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})

(defn- listing-response
  [heading posts]
  (html-response 200 (layout/page {:title heading
                                    :body  (index-view/listing-page {:heading heading :posts posts})})))

(defn index
  [_request]
  (listing-response "Comic Blog" (store/articles)))

(defn news-list
  [_request]
  (listing-response "News" (store/by-type :news)))

(defn reviews-list
  [_request]
  (listing-response "Reviews" (store/by-type :review)))

(defn- by-release-date
  "Ascending by release date, oldest first -- collections with no known
   release date (nothing announced yet) sort after every dated one."
  [a b]
  (let [ra (:release-date a)
        rb (:release-date b)]
    (cond
      (and ra rb) (compare ra rb)
      ra          -1
      rb          1
      :else       0)))

(defn collections-list
  [_request]
  (listing-response "Volumes" (sort by-release-date (store/by-type :collection))))

(defn creators-list
  [_request]
  (listing-response "Creators" (store/by-type :creator)))

(defn- title-sort-key
  "Alphabetizes on a title while ignoring a leading \"The \", so \"The
   Flash\" sorts under F rather than T."
  [{:keys [title]}]
  (-> title (str/replace #"(?i)^the\s+" "") str/lower-case))

(defn lines-list
  [_request]
  (listing-response "DC Finest Lines" (sort-by title-sort-key (store/by-type :line))))

(defn tag-index
  [_request]
  (let [tags (store/all-tags)]
    (html-response 200
      (layout/page
        {:title "Tags"
         :body  [:section.listing
                 [:h1 "Tags"]
                 [:ul.tag-index
                  (for [tag tags]
                    [:li [:a {:href (str "/tags/" tag)} tag]])]]}))))

(defn tag-list
  [request]
  (let [tag (get-in request [:path-params :tag])]
    (listing-response (str "Tag: " tag) (store/by-tag tag))))

(defn- creator-role-on
  [collection creator-slug]
  (some #(when (= creator-slug (:slug %)) (:role %)) (:creators collection)))

(defn- resolve-creators
  [creators]
  (keep (fn [{:keys [slug role]}] (some-> (store/by-slug slug) (assoc :role role))) creators))

(defn- resolve-issue-creators
  [issues]
  (map (fn [issue]
         (cond-> issue
           (seq (:creators issue)) (assoc :creators (resolve-creators (:creators issue)))))
       issues))

(defn- enrich
  [{:keys [type slug collections creators issues] :as post}]
  (cond-> post
    (seq collections)     (assoc :referenced-collections (keep store/by-slug collections))
    (= type :collection)  (assoc :related (store/referencing slug)
                                  :creator-credits (resolve-creators creators))
    (seq issues)          (assoc :issues (resolve-issue-creators issues))
    (= type :creator)     (assoc :credited-collections
                                  (map #(assoc % :role (creator-role-on % slug)) (store/credited-on slug)))
    (= type :line)        (assoc :line-collections (store/under-line slug))))

(defn post-page
  [request]
  (let [slug (get-in request [:path-params :slug])
        post (store/by-slug slug)]
    (if post
      (let [enriched (enrich post)]
        (html-response 200 (layout/page {:title (components/display-title enriched)
                                          :body  (post-view/post-page enriched)})))
      (html-response 404 (layout/page {:title "Not Found" :body [:p "Post not found."]})))))

(defn not-found
  [_request]
  (html-response 404 (layout/page {:title "Not Found" :body [:p "Page not found."]})))
