(ns finest.handler
  (:require [clojure.string :as str]
            [finest.content.store :as store]
            [finest.views.layout :as layout]
            [finest.views.index :as index-view]
            [finest.views.post :as post-view]
            [finest.views.components :as components])
  (:import (java.time LocalDate)))

(defn- html-response
  [status body]
  {:status  status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})

(defn- page
  "Every page carries the release sidebar, judged against today's date."
  [opts]
  (layout/page (assoc opts :sidebar (store/release-window (LocalDate/now) 3))))

(defn- listing-response
  ([heading posts] (listing-response heading posts nil nil))
  ([heading posts card] (listing-response heading posts card nil))
  ([heading posts card list-class]
   (html-response 200 (page {:title heading
                                     :body  (index-view/listing-page
                                              (cond-> {:heading heading :posts posts}
                                                card       (assoc :card card)
                                                list-class (assoc :list-class list-class)))}))))

(defn index
  [_request]
  (html-response 200 (page {:body (index-view/listing-page
                                           {:heading "Latest" :posts (store/articles)})})))

(defn news-list
  [_request]
  (listing-response "News" (store/articles)))

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

(defn- attach-review
  "The review (if any) of this collection, for volume-row's third column."
  [collection]
  (if-let [review (store/review-for (:slug collection))]
    (assoc collection :review review)
    collection))

(defn collections-list
  [_request]
  (listing-response "Volumes" (mapv attach-review (sort by-release-date (store/by-type :collection))) components/volume-row))

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
  (listing-response "Lines" (sort-by title-sort-key (store/by-type :line)) components/line-row "line-columns"))

(defn tag-index
  [_request]
  (let [tags (store/all-tags)]
    (html-response 200
      (page
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

(defn- related-lines
  "Other lines this collection connects to via a shared split issue --
   e.g. a Catwoman issue also collected in an Events volume. Only
   siblings actually filed under a (different) line count; a standalone
   sibling with no line, or one on this same line, is skipped."
  [own-line issues]
  (distinct
    (for [{:keys [number siblings]} issues
          {sib-line :line sib-line-title :line-title} siblings
          :when (and sib-line (not= sib-line own-line))]
      {:line-slug sib-line :line-title sib-line-title :number number})))

(defn- enrich
  [{:keys [type slug line collections creators issues] :as post}]
  (cond-> post
    (seq collections)     (assoc :referenced-collections (keep store/by-slug collections))
    (= type :collection)  (assoc :related (store/referencing slug)
                                  :creator-credits (resolve-creators creators)
                                  :related-lines (related-lines line issues))
    (seq issues)          (assoc :issues (resolve-issue-creators issues))
    (= type :creator)     (assoc :credited-collections
                                  (map #(assoc % :role (creator-role-on % slug)) (store/credited-on slug)))
    (= type :line)        (assoc :line-collections (mapv attach-review (store/under-line slug)))))

(defn post-page
  [request]
  (let [slug (get-in request [:path-params :slug])
        post (store/by-slug slug)]
    (if post
      (let [enriched (enrich post)]
        (html-response 200 (page {:title (components/display-title enriched)
                                          :body  (post-view/post-page enriched)})))
      (html-response 404 (page {:title "Not Found" :body [:p "Post not found."]})))))

(defn not-found
  [_request]
  (html-response 404 (page {:title "Not Found" :body [:p "Page not found."]})))
