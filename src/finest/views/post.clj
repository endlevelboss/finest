(ns finest.views.post
  (:require [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn- collection-refs
  [collections]
  [:div.post-comics
   [:span.post-comics-label "About: "]
   (interpose ", " (for [{:keys [slug title]} collections]
                      [:a {:href (str "/posts/" slug)} title]))])

(defn- creator-credits-block
  [creators]
  [:div.creator-credits
   [:span.post-comics-label "Creators: "]
   (interpose ", " (for [{:keys [slug title role]} creators]
                      [:span [:a {:href (str "/posts/" slug)} title] (when role (str " — " role))]))])

(defn- related-articles
  [posts]
  (when (seq posts)
    [:section.related
     [:h2 "Reviews & News"]
     (map c/post-card posts)]))

(defn- credited-collections-block
  [collections]
  (when (seq collections)
    [:section.related
     [:h2 "Collections"]
     (for [{:keys [role] :as collection} collections]
       [:div.credited-collection
        (c/post-card collection)
        (when role [:p.credited-role role])])]))

(defn post-page
  [{:keys [title date type tags rating html cover
           referenced-collections related creator-credits credited-collections]}]
  [:article.post
   (when cover [:img.cover-hero {:src cover :alt title}])
   [:h1 title]
   [:div.post-meta
    (when date [:span.post-date (str date)])
    (when (#{:news :review} type) (c/type-badge type))
    (when (= type :review) (c/rating-stars rating))]
   (when (seq referenced-collections) (collection-refs referenced-collections))
   (when (seq creator-credits) (creator-credits-block creator-credits))
   [:div.post-tags (map c/tag-pill tags)]
   [:div.post-body (h/raw html)]
   (related-articles related)
   (credited-collections-block credited-collections)])
