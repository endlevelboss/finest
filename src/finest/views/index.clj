(ns finest.views.index
  (:require [finest.views.components :as c]))

(defn listing-page
  [{:keys [heading posts card list-class] :or {card c/post-card}}]
  [:section.listing
   [:h1 heading]
   (if (seq posts)
     [:div {:class list-class} (map card posts)]
     [:p.empty "No posts yet."])])
