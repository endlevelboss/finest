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
   their own title as the big heading, with the line's name as a smaller
   subtitle underneath, linked to the line's own page. Reviews of a
   volume get an \"A review of: ...\" kicker above their own title."
  [{:keys [title line line-title review-of] :as post}]
  (cond
    line-title
    [:div.post-heading
     [:h1 title]
     [:p.post-subtitle [:a {:href (str "/posts/" line)} line-title]]]

    (seq review-of)
    [:div.post-heading
     (c/review-of-kicker post)
     [:h1 title]]

    :else
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

(defn- part-note
  [part]
  (when part [:span.issue-part (str " (" part ")")]))

(defn- issue-siblings-block
  [siblings]
  [:div.issue-siblings
   [:span.post-comics-label "Also collected in: "]
   (interpose ", " (for [s siblings]
                      [:span [:a {:href (str "/posts/" (:slug s))} (c/display-title s)]
                       (part-note (:part s))]))])

(defn- double-dip-entry
  [{:keys [issue part date creators siblings]}]
  [:li.issue-entry
   [:span.issue-number issue]
   (part-note part)
   (when date [:span.issue-date (str " — " date)])
   (when (seq creators) [:span.issue-creators (str " (" (issue-creators-text creators) ")")])
   (when (seq siblings) (issue-siblings-block siblings))])

(defn- double-dip-list
  "Issues (or parts of issues) this volume shares with other volumes --
   not a full contents list, just the overlap a collector would want to
   know about before buying both."
  [dips]
  (when (seq dips)
    [:section.related
     [:h2 "Double-dips"]
     [:p.section-note "Issues also reprinted in other volumes."]
     [:ol.issue-list (map double-dip-entry dips)]]))

(defn- post-head
  "Heading, meta line, credits and tags. A volume's cover sits beside all
   that, right-aligned and scaled down; other posts keep a full-width hero
   above it."
  [{:keys [title date-display type tags rating cover
           referenced-collections creator-credits review-of] :as post}]
  (let [head [(post-heading post)
              [:div.post-meta
               (when date-display [:span.post-date date-display])
               (c/release-date-note post)
               (when (#{:news :review} type) (c/type-badge type))
               (when (= type :review) (c/rating-stars rating))]
              ;; the review-of kicker already links the volumes a review covers
              (when (and (seq referenced-collections) (empty? review-of))
                (collection-refs referenced-collections))
              (when (seq creator-credits) (creator-credits-block creator-credits))
              [:div.post-tags (map c/tag-pill tags)]]]
    (if (and cover (= type :collection))
      [:header.volume-head
       (into [:div.volume-head-text] head)
       [:img.volume-cover {:src cover :alt title}]]
      (list* (when cover [:img.cover-hero {:src cover :alt title}]) head))))

(defn post-page
  [{:keys [html related credited-collections line-collections double-dips] :as post}]
  [:article.post
   (post-head post)
   [:div.post-body (h/raw html)]
   (double-dip-list double-dips)
   (listing-section "Reviews & News" related)
   (credited-collections-block credited-collections)
   (listing-section "Volumes" line-collections c/volume-row-with-thumb)])
