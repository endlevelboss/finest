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
  [heading posts]
  (when (seq posts)
    [:section.related
     [:h2 heading]
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
  [{:keys [title date-display type tags rating html cover
           referenced-collections related creator-credits credited-collections
           line-collections] :as post}]
  [:article.post
   (when cover [:img.cover-hero {:src cover :alt title}])
   (post-heading post)
   [:div.post-meta
    (when date-display [:span.post-date date-display])
    (when (#{:news :review} type) (c/type-badge type))
    (when (= type :review) (c/rating-stars rating))]
   (when (seq referenced-collections) (collection-refs referenced-collections))
   (when (seq creator-credits) (creator-credits-block creator-credits))
   [:div.post-tags (map c/tag-pill tags)]
   [:div.post-body (h/raw html)]
   (listing-section "Reviews & News" related)
   (credited-collections-block credited-collections)
   (listing-section "Collections" line-collections)])
