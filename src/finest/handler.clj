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
  (listing-response "Comic Blog" (store/all-posts)))

(defn news-list
  [_request]
  (listing-response "News" (store/by-type :news)))

(defn reviews-list
  [_request]
  (listing-response "Reviews" (store/by-type :review)))

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

(defn post-page
  [request]
  (let [slug (get-in request [:path-params :slug])
        post (store/by-slug slug)]
    (if post
      (html-response 200 (layout/page {:title (:title post) :body (post-view/post-page post)}))
      (html-response 404 (layout/page {:title "Not Found" :body [:p "Post not found."]})))))

(defn not-found
  [_request]
  (html-response 404 (layout/page {:title "Not Found" :body [:p "Page not found."]})))
