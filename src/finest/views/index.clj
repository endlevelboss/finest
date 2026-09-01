(ns finest.views.index
  (:require [finest.views.components :as c]))

(defn listing-page
  [{:keys [heading posts]}]
  [:section.listing
   [:h1 heading]
   (if (seq posts)
     (map c/post-card posts)
     [:p.empty "No posts yet."])])
