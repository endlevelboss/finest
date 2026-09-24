(ns finest.views.components
  (:require [clojure.string :as str]
            [finest.content.post :as post])
  (:import (java.time LocalDate)))

(def site-name "Transmissions")

(defn banner
  "The title banner at the top of every page, which scrolls away above the
   sticky nav. The blocks and dot patch on the right echo the design
   system's cover."
  []
  [:header.banner
   [:div.banner-text
    [:p.banner-title [:a {:href "/"} site-name]]
    [:p.banner-tagline "Reading the DC Finest line, one brick at a time."]]
   [:div.banner-art {:aria-hidden "true"}
    [:span.block.tint [:span.halftone]]
    [:span.block.yellow]
    [:span.block.deep]
    [:span.block.blue]
    [:span.block.red]]])

(defn nav
  []
  [:nav.site-nav
   [:a {:href "/news"} "News"]
   [:a {:href "/lines"} "Lines"]
   [:a {:href "/collections"} "Volumes"]])

(defn tag-pill
  [tag]
  [:a.tag-pill {:href (str "/tags/" tag)} tag])

(defn- format-rating
  "4.0 -> \"4\", 4.5 -> \"4.5\" -- drop a trailing .0 for whole-number ratings."
  [rating]
  (let [s (str rating)]
    (if (str/ends-with? s ".0") (subs s 0 (- (count s) 2)) s)))

(defn rating-stars
  "Renders a rating out of `max` (default 3) as filled/empty stars,
   with a textual label for accessibility since partial stars aren't
   conveyed by the glyphs alone."
  ([rating] (rating-stars rating 3))
  ([rating max]
   [:span.rating {:aria-label (str rating " out of " max " stars")}
    (for [i (range 1 (inc max))]
      (let [filled? (<= i (Math/round (double rating)))]
        [:span.star {:class (if filled? "filled" "empty") :aria-hidden "true"} "★"]))
    [:span.rating-value (str " " (format-rating rating) "/" max)]]))

(def ^:private badge-labels
  "The :news type covers more than breaking news -- any non-review writeup,
   op-eds included -- so its badge reads as the more generic \"Article\"."
  {:news "Article"})

(defn type-badge
  [type]
  [:span.type-badge {:class (name type)} (or (badge-labels type) (str/capitalize (name type)))])

(defn display-title
  "A collection's own title is just its distinguishing name (\"Year One\");
   when it belongs to a line, the combined heading leads with the line's
   name (\"Batman: Year One\")."
  [{:keys [title line-title]}]
  (if line-title (str line-title ": " title) title))

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
   (release-list "Coming soon" upcoming true)])

(defn- volume-review
  "A dedicated review column: a link with the review's own title plus its
   star rating -- blank (nil, no placeholder) when there isn't one yet."
  [review]
  (when review
    [:a.volume-review {:href (str "/posts/" (:slug review))}
     [:span.volume-review-title (:title review)]
     (rating-stars (:rating review))]))

(defn volume-row
  "A compact, table-like row for the Volumes listing: title, date range,
   and review (if any) as three columns on the first line, with the
   release date on a second line below -- no tags, no cover art, nothing
   slug-shaped."
  [{:keys [slug date-display review] :as post}]
  [:div.volume-row
   [:div.volume-row-main
    [:a.volume-title {:href (str "/posts/" slug)} (display-title post)]
    [:span.volume-range date-display]
    (volume-review review)]
   (release-date-note post)])

(defn line-row
  "A compact single-line row for the Lines listing -- just the clickable
   name, no meta line or tags, since a line carries none of the
   date/rating/badge info a post-card is built to show."
  [{:keys [slug] :as post}]
  [:div.line-row
   [:a.line-title {:href (str "/posts/" slug)} (display-title post)]])

(defn post-card
  [{:keys [slug title date-display type tags rating cover] :as post}]
  [:article.post-card
   [:div.post-card-body
    [:h2 [:a {:href (str "/posts/" slug)} (display-title post)]]
    [:div.post-meta
     (when date-display [:span.post-date date-display])
     (release-date-note post)
     (when (#{:news :review} type) (type-badge type))
     (when (= type :review) (rating-stars rating))]
    [:div.post-tags (map tag-pill tags)]]
   (when cover
     [:a.cover-thumb-link {:href (str "/posts/" slug)}
      [:img.cover-thumb {:src cover :alt title :loading "lazy"}]])])
