(ns finest.views.post
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn- collection-refs
  [collections]
  [:div.post-comics
   [:span.post-comics-label "About: "]
   (interpose ", " (for [{:keys [slug title]} collections]
                      [:a {:href (str "/posts/" slug)} title]))])

(defn- creator-credits-block
  "Creator names are plain text for now, not links -- the creator profile
   pages are a feature we're holding off on surfacing until it's fleshed
   out further."
  [creators]
  [:div.creator-credits
   [:span.post-comics-label "Creators: "]
   (interpose ", " (for [{:keys [title role]} creators]
                      [:span title (when role (str " — " role))]))])

(defn- post-heading
  "A plain <h1> for most posts. Collections that belong to a line lead with
   the line's name as the big title, linked to the line's own page, and show
   their own distinguishing name as a subtitle underneath."
  [{:keys [title line line-title]}]
  (if line-title
    [:div.post-heading
     [:h1 [:a {:href (str "/posts/" line)} line-title]]
     [:p.post-subtitle title]]
    [:h1 title]))

(defn- listing-section
  ([heading posts] (listing-section heading posts c/post-card))
  ([heading posts card]
   (when (seq posts)
     [:section.related
      [:h2 heading]
      (map card posts)])))

(defn- credited-collections-block
  [collections]
  (when (seq collections)
    [:section.related
     [:h2 "Volumes"]
     (for [{:keys [role] :as collection} collections]
       [:div.credited-collection
        (c/post-card collection)
        (when role [:p.credited-role role])])]))

(defn- issue-creators-text
  [creators]
  (str/join ", " (map (fn [{:keys [title role]}] (if role (str title " — " role) title)) creators)))

(defn- issue-siblings-block
  [siblings]
  [:div.issue-siblings
   [:span.post-comics-label "Also collected in: "]
   (interpose ", " (for [s siblings]
                      [:a {:href (str "/posts/" (:slug s))} (c/display-title s)]))])

(defn- issue-entry
  [{:keys [number date creators siblings]}]
  [:li.issue-entry
   [:span.issue-number (str number)]
   (when date [:span.issue-date (str " — " date)])
   (when (seq creators) [:span.issue-creators (str " (" (issue-creators-text creators) ")")])
   (when (seq siblings) (issue-siblings-block siblings))])

(defn- issue-list
  [issues]
  (when (seq issues)
    [:section.related
     [:h2 "Issues"]
     [:ol.issue-list (map issue-entry issues)]]))

(defn post-page
  [{:keys [title date-display type tags rating html cover
           referenced-collections related creator-credits credited-collections
           line-collections issues] :as post}]
  [:article.post
   (when cover [:img.cover-hero {:src cover :alt title}])
   (post-heading post)
   [:div.post-meta
    (when date-display [:span.post-date date-display])
    (c/release-date-note post)
    (when (#{:news :review} type) (c/type-badge type))
    (when (= type :review) (c/rating-stars rating))]
   (when (seq referenced-collections) (collection-refs referenced-collections))
   (when (seq creator-credits) (creator-credits-block creator-credits))
   [:div.post-tags (map c/tag-pill tags)]
   [:div.post-body (h/raw html)]
   (issue-list issues)
   (listing-section "Reviews & News" related)
   (credited-collections-block credited-collections)
   (listing-section "Volumes" line-collections c/volume-row)])
