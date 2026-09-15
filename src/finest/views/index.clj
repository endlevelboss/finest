(ns finest.views.index
  (:require [finest.views.components :as c]))

(defn listing-page
  [{:keys [heading posts card] :or {card c/post-card}}]
  [:section.listing
   [:h1 heading]
   (if (seq posts)
     (map card posts)
     [:p.empty "No posts yet."])])
