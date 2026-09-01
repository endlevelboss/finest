(ns finest.views.post
  (:require [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn post-page
  [{:keys [title date type tags rating html cover]}]
  [:article.post
   (when cover [:img.cover-hero {:src cover :alt title}])
   [:h1 title]
   [:div.post-meta
    [:span.post-date (str date)]
    (c/type-badge type)
    (when (= type :review) (c/rating-stars rating))]
   [:div.post-tags (map c/tag-pill tags)]
   [:div.post-body (h/raw html)]])
