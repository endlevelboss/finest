(ns finest.handler
  (:require [finest.content.store :as store]
            [finest.views.layout :as layout]
            [finest.views.index :as index-view]
            [finest.views.post :as post-view]))

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

(defn collections-list
  [_request]
  (listing-response "Collections" (store/by-type :collection)))

(defn creators-list
  [_request]
  (listing-response "Creators" (store/by-type :creator)))

(defn lines-list
  [_request]
  (listing-response "Lines" (store/by-type :line)))

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

(defn- enrich
  [{:keys [type slug collections creators line] :as post}]
  (cond-> post
    (seq collections)     (assoc :referenced-collections (keep store/by-slug collections))
    (= type :collection)  (assoc :related (store/referencing slug)
                                  :creator-credits (keep (fn [{:keys [slug role]}]
                                                            (some-> (store/by-slug slug) (assoc :role role)))
                                                          creators))
    (and (= type :collection) line) (assoc :line-ref (store/by-slug line))
    (= type :creator)     (assoc :credited-collections
                                  (map #(assoc % :role (creator-role-on % slug)) (store/credited-on slug)))
    (= type :line)        (assoc :line-collections (store/under-line slug))))

(defn post-page
  [request]
  (let [slug (get-in request [:path-params :slug])
        post (store/by-slug slug)]
    (if post
      (html-response 200 (layout/page {:title (:title post) :body (post-view/post-page (enrich post))}))
      (html-response 404 (layout/page {:title "Not Found" :body [:p "Post not found."]})))))

(defn not-found
  [_request]
  (html-response 404 (layout/page {:title "Not Found" :body [:p "Page not found."]})))
