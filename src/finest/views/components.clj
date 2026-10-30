(ns finest.views.components
  (:require [clojure.string :as str]
            [finest.content.post :as post])
  (:import (java.time LocalDate)))

(def site-name "Transmissions")

;; placeholder until there's a final one -- used as the description of
;; the home page and listings, and as the feed's subtitle
(def site-tagline "Reading the DC Finest line, one brick at a time.")

(def ^:private first-year 2026)

(defn copyright-years
  "\"2026\", or \"2026–2028\" once the site has been around a while."
  []
  (let [year (.getYear (LocalDate/now))]
    (if (> year first-year) (str first-year "–" year) (str first-year))))

(defn banner
  "The title banner at the top of every page, which scrolls away above the
   sticky nav. The artwork's sky blue carries on past the image's edge."
  []
  [:header.banner
   [:a.banner-link {:href "/"}
    [:img.banner-image {:src "/images/banner.png" :width 1602 :height 624
                        :alt "Transmissions from Earth Q"}]]])

(defn nav
  []
  [:nav.site-nav
   [:a {:href "/"} "Transmissions"]
   [:a {:href "/reviews"} "Reviews"]
   [:a {:href "/articles"} "Articles"]
   [:a {:href "/lines"} "Lines"]
   [:a {:href "/released"} "Released"]
   [:a {:href "/upcoming"} "Upcoming"]])

(defn tag-pill
  [tag]
  [:a.tag-pill {:href (str "/tags/" tag)} tag])

(def ^:private badge-labels
  "The :news type covers more than breaking news -- any non-review writeup,
   op-eds included -- so its badge reads as the more generic \"Article\"."
  {:news "Article"})

(defn type-badge
  [type]
  [:span.type-badge {:class (name type)} (or (badge-labels type) (str/capitalize (name type)))])

(defn draft-badge
  "Marks a draft -- only ever seen in dev, since drafts aren't loaded
   on the production server."
  [{:keys [draft]}]
  (when draft [:span.type-badge.draft "Draft"]))

(defn display-title
  "A collection's own title is just its distinguishing name (\"Year One\");
   when it belongs to a line, the combined heading leads with the line's
   name (\"Batman: Year One\")."
  [{:keys [title line-title]}]
  (if line-title (str line-title ": " title) title))

(defn- split-title-links
  "A volume's combined title with its two halves linked separately -- the
   line's name to the line, its own title to itself, a grey colon between.
   A volume outside a line is just one link."
  [{:keys [slug title line line-title] :as v}]
  (if (and line line-title)
    (list [:a {:href (str "/posts/" line)} line-title] [:span.title-sep ":"]
          [:a {:href (str "/posts/" slug)} title])
    [:a {:href (str "/posts/" slug)} (display-title v)]))

(defn- by-line
  "The volumes a review covers, grouped by line so each line's name is
   written once -- groups in order of first appearance, volumes in their
   frontmatter order within each. A volume outside a line is a group of
   its own."
  [volumes]
  (let [k      (fn [v] (or (:line-title v) [::own (:slug v)]))
        groups (group-by k volumes)]
    (map groups (distinct (map k volumes)))))

(defn- line-group-links
  "\"Batman: Year One, The Killing Joke\" -- the line linked once, then
   each volume's own title."
  [[{:keys [line line-title]} :as group]]
  (if line-title
    (list [:a {:href (str "/posts/" line)} line-title] [:span.title-sep ":"]
          (interpose ", " (for [{:keys [slug title]} group]
                            [:a {:href (str "/posts/" slug)} title])))
    (split-title-links (first group))))

(defn- line-group-text
  [[{:keys [line-title]} :as group]]
  (if line-title
    (str line-title ": " (str/join ", " (map :title group)))
    (display-title (first group))))

(defn review-of-kicker
  "\"A review of: Batman: Year One, The Killing Joke; Superman: The Man of
   Steel\" above a review's own title, linking each volume it covers (and
   its line, once per line). Nil for anything that isn't one."
  [{:keys [review-of]}]
  (when (seq review-of)
    [:p.review-of "A review of: "
     (interpose "; " (map line-group-links (by-line review-of)))]))

(defn review-of-text
  "The kicker as plain text, for places that are already one big link."
  [{:keys [review-of]}]
  (str/join "; " (map line-group-text (by-line review-of))))

(defn release-date-note
  "\"Released Nov 5, 2024\" once it's out, \"Releases Jun 1, 2027\" while
   it's still upcoming -- judged against today at render time, and the
   class hook lets upcoming releases be styled differently."
  ([post] (release-date-note post (LocalDate/now)))
  ([{:keys [release-date-display] :as post} today]
   (when release-date-display
     (let [upcoming? (post/upcoming? post today)]
       [:span.release-date {:class (when upcoming? "upcoming")}
        (str (if upcoming? "Releases " "Released ") release-date-display)]))))

(defn- release-list
  [heading volumes upcoming?]
  (when (seq volumes)
    [:section.sidebar-box
     [:h2.sidebar-heading heading]
     [:ol.release-list
      (for [{:keys [slug release-date-display] :as v} volumes]
        [:li
         [:a.release-title {:href (str "/posts/" slug)} (display-title v)]
         [:span.release-date {:class (when upcoming? "upcoming")} release-date-display]])]]))

(defn sidebar
  "The right-hand column: the latest Finest volumes out and the next ones
   due, each with its street date."
  [{:keys [recent upcoming]}]
  [:aside.sidebar
   (release-list "Recently released" recent false)
   (release-list "Next up" upcoming true)])

(defn- volume-review
  "A dedicated review column: a link with the review's own title -- blank
   (nil, no placeholder) when there isn't one yet."
  [review]
  (when review
    [:a.volume-review {:href (str "/posts/" (:slug review))}
     [:span.volume-review-title (:title review)]]))

(defn- volume-row*
  [{:keys [slug date-display review] :as post} link-line?]
  [:div.volume-row
   [:div.volume-row-main
    (if link-line?
      [:span.volume-title (split-title-links post)]
      [:a.volume-title {:href (str "/posts/" slug)} (display-title post)])
    [:span.volume-range date-display]
    (volume-review review)]
   (release-date-note post)])

(defn volume-row
  "A compact, table-like row for the Volumes listing: title, date range,
   and review (if any) as three columns on the first line, with the
   release date on a second line below -- no tags, no cover art, nothing
   slug-shaped. The line's name links to the line, so the full list can
   be browsed by line."
  [post]
  (volume-row* post true))

(defn line-volume-row
  "volume-row for a line's own volume listing. The title stays one link,
   since the line it would point to is this very page."
  [post]
  (volume-row* post false))

(defn line-row
  "A compact single-line row for the Lines listing -- just the clickable
   name, no meta line or tags, since a line carries none of the
   date/badge info a post-card is built to show."
  [{:keys [slug] :as post}]
  [:div.line-row
   [:a.line-title {:href (str "/posts/" slug)} (display-title post)]])

(defn review-embed
  "A compact review box dropped into an article body with {{review slug}}.
   The whole box is one link, so the \"A review of:\" volumes are plain
   text here rather than the kicker's own nested links."
  [{:keys [slug review-of feature-image cover title] :as review}]
  [:a.review-embed {:href (str "/posts/" slug)}
   [:span.review-embed-body
    (when (seq review-of)
      [:span.review-of (str "A review of: " (review-of-text review))])
    [:span.review-embed-title (display-title review)]]
   (cond
     feature-image [:span.post-card-feature
                    [:img {:src (:src feature-image) :alt (:alt feature-image) :loading "lazy"}]]
     cover         [:span.post-card-feature [:img {:src cover :alt title :loading "lazy"}]])])

(defn post-card
  "A listing card. A post with a featured image (marked in its body) shows
   it as a cropped thumbnail on the right, in place of the cover thumb."
  [{:keys [slug title date-display type tags cover feature-image] :as post}]
  [:article.post-card
   [:div.post-card-body
    (review-of-kicker post)
    ;; the title link is stretched over the whole card (see style.css), so
    ;; without this any press-and-move inside the card drags the link
    [:h2 [:a {:href (str "/posts/" slug) :draggable "false"} (display-title post)]]
    [:div.post-meta
     (when date-display [:span.post-date date-display])
     (release-date-note post)
     (draft-badge post)
     (when (#{:news :review} type) (type-badge type))]
    [:div.post-tags (map tag-pill tags)]]
   (when feature-image
     [:a.post-card-feature {:href (str "/posts/" slug) :tabindex "-1" :aria-hidden "true"}
      [:img {:src (:src feature-image) :alt (:alt feature-image) :loading "lazy"}]])
   (when (and cover (not feature-image))
     [:a.cover-thumb-link {:href (str "/posts/" slug)}
      [:img.cover-thumb {:src cover :alt title :loading "lazy"}]])])
