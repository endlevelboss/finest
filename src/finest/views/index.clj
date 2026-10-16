(ns finest.views.index
  (:require [finest.views.components :as c]))

(defn- page-href
  "Page 1 is the listing's plain address, so existing links keep working."
  [path n]
  (if (= n 1) path (str path "?page=" n)))

(defn- pager-nav
  "Newer/Older links under a paged listing -- nothing when it fits on one page."
  [{:keys [page pages path]}]
  (when (> pages 1)
    [:nav.pager {:aria-label "Pages"}
     (if (> page 1)
       [:a.pager-newer {:href (page-href path (dec page)) :rel "prev"} "← Newer"]
       [:span])
     [:span.pager-count (str "Page " page " of " pages)]
     (if (< page pages)
       [:a.pager-older {:href (page-href path (inc page)) :rel "next"} "Older →"]
       [:span])]))

(defn listing-page
  [{:keys [heading posts card list-class pager] :or {card c/post-card}}]
  [:section.listing
   [:h1 heading]
   (if (seq posts)
     [:div {:class list-class} (map card posts)]
     [:p.empty "No posts yet."])
   (when pager (pager-nav pager))])

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
