(ns finest.views.components
  (:require [clojure.string :as str]))

(defn nav
  []
  [:nav.site-nav
   [:a {:href "/"} "Comic Blog"]
   [:a {:href "/news"} "News"]
   [:a {:href "/reviews"} "Reviews"]
   [:a {:href "/collections"} "Collections"]
   [:a {:href "/creators"} "Creators"]
   [:a {:href "/lines"} "Lines"]
   [:a {:href "/tags"} "Tags"]])

(defn tag-pill
  [tag]
  [:a.tag-pill {:href (str "/tags/" tag)} tag])

(defn- format-rating
  "4.0 -> \"4\", 4.5 -> \"4.5\" -- drop a trailing .0 for whole-number ratings."
  [rating]
  (let [s (str rating)]
    (if (str/ends-with? s ".0") (subs s 0 (- (count s) 2)) s)))

(defn rating-stars
  "Renders a rating out of `max` (default 5) as filled/empty stars,
   with a textual label for accessibility since partial stars aren't
   conveyed by the glyphs alone."
  ([rating] (rating-stars rating 5))
  ([rating max]
   [:span.rating {:aria-label (str rating " out of " max " stars")}
    (for [i (range 1 (inc max))]
      (let [filled? (<= i (Math/round (double rating)))]
        [:span.star {:class (if filled? "filled" "empty") :aria-hidden "true"} "★"]))
    [:span.rating-value (str " " (format-rating rating) "/" max)]]))

(defn type-badge
  [type]
  [:span.type-badge {:class (name type)} (str/capitalize (name type))])

(defn display-title
  "A collection's own title is just its distinguishing name (\"Year One\");
   when it belongs to a line, the combined heading leads with the line's
   name (\"Batman: Year One\")."
  [{:keys [title line-title]}]
  (if line-title (str line-title ": " title) title))

(defn release-date-note
  "\"Released Nov 5, 2024\" once it's out, \"Releases Jun 1, 2027\" while
   it's still upcoming -- the class hook lets upcoming releases be styled
   differently."
  [{:keys [release-date-display release-upcoming?]}]
  (when release-date-display
    [:span.release-date {:class (when release-upcoming? "upcoming")}
     (str (if release-upcoming? "Releases " "Released ") release-date-display)]))

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
