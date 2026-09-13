(ns finest.views.post
  (:require [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn- comic-refs
  [comics]
  [:div.post-comics
   [:span.post-comics-label "About: "]
   (interpose ", " (for [{:keys [slug title]} comics]
                      [:a {:href (str "/posts/" slug)} title]))])

(defn- related-articles
  [posts]
  (when (seq posts)
    [:section.related
     [:h2 "Reviews & News"]
     (map c/post-card posts)]))

(defn post-page
  [{:keys [title date type tags rating html cover referenced-comics related]}]
  [:article.post
   (when cover [:img.cover-hero {:src cover :alt title}])
   [:h1 title]
   [:div.post-meta
    [:span.post-date (str date)]
    (c/type-badge type)
    (when (= type :review) (c/rating-stars rating))]
   (when (seq referenced-comics) (comic-refs referenced-comics))
   [:div.post-tags (map c/tag-pill tags)]
   [:div.post-body (h/raw html)]
   (related-articles related)])
