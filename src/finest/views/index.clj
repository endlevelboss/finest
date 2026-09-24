(ns finest.views.index
  (:require [finest.views.components :as c]))

(defn listing-page
  [{:keys [heading posts card list-class] :or {card c/post-card}}]
  [:section.listing
   [:h1 heading]
   (if (seq posts)
     [:div {:class list-class} (map card posts)]
     [:p.empty "No posts yet."])])

(defn grouped-listing-page
  "A listing split under subheadings, e.g. volumes by release year.
   `groups` is an ordered seq of [subheading posts]."
  [{:keys [heading groups card]}]
  [:section.listing
   [:h1 heading]
   (if (seq groups)
     (for [[subheading posts] groups]
       [:section.listing-group
        [:h2.listing-group-heading subheading]
        (map card posts)])
     [:p.empty "No posts yet."])])
