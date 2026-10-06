(ns finest.views.embeds
  "Shortcuts an author can write in a post body to point at other posts,
   expanded at render time once every post is known:

     [[some-slug]]           an inline link titled with that post's title
                             (plus its stars, for a review)
     {{review some-slug}}    on a line of its own, a compact review box

   A slug that doesn't resolve is left as written, so a typo stays visible
   in the article rather than silently vanishing."
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [finest.views.components :as c]))

(def ^:private review-card-pattern
  ;; markdown wraps a line on its own in a <p>, which a block box can't
  ;; sit inside, so the whole paragraph is swapped out
  #"<p>\{\{\s*review\s+([a-z0-9-]+)\s*\}\}</p>")

(def ^:private post-link-pattern
  #"\[\[([a-z0-9-]+)\]\]")

(defn- render
  [hiccup]
  (str (h/html hiccup)))

(defn- post-link
  [{:keys [slug type rating] :as post}]
  [:span.post-link
   [:a {:href (str "/posts/" slug)} (c/display-title post)]
   (when (= type :review) (c/rating-stars rating))])

(defn expand
  "Expands review cards and inline post links in a rendered post body.
   `lookup` maps a slug to its post, or nil."
  [html lookup]
  (-> html
      (str/replace review-card-pattern
                   (fn [[match slug]]
                     (let [post (lookup slug)]
                       (if (= :review (:type post))
                         (render (c/review-embed post))
                         match))))
      (str/replace post-link-pattern
                   (fn [[match slug]]
                     (if-let [post (lookup slug)]
                       (render (post-link post))
                       match)))))
