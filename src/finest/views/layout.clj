(ns finest.views.layout
  (:require [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn- meta-tags
  "The description for search results, and the Open Graph and Twitter
   tags apps read to build a link preview card. All URLs are absolute."
  [{:keys [title description image url type]}]
  (list
    [:meta {:name "description" :content description}]
    [:meta {:property "og:site_name" :content c/site-name}]
    [:meta {:property "og:type" :content type}]
    [:meta {:property "og:title" :content title}]
    [:meta {:property "og:description" :content description}]
    [:meta {:property "og:url" :content url}]
    [:meta {:property "og:image" :content image}]
    [:meta {:name "twitter:card" :content "summary_large_image"}]))

(defn page
  "`meta` is {:title :description :image :url :type} for meta-tags."
  [{:keys [title body sidebar meta]}]
  (str
    (h/html
      (h/raw "<!DOCTYPE html>\n")
      [:html {:lang "en"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title (if title (str title " — " c/site-name) c/site-name)]
        (when meta (meta-tags meta))
        [:link {:rel "icon" :href "/favicon.ico" :sizes "16x16 32x32 48x48"}]
        [:link {:rel "icon" :type "image/png" :href "/icon-512.png" :sizes "512x512"}]
        [:link {:rel "apple-touch-icon" :href "/apple-touch-icon.png"}]
        [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin ""}]
        [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css2?family=Anton&family=Archivo:wght@400;600;700&family=Source+Serif+4:ital,wght@0,400;0,600;1,400&display=swap"}]
        [:link {:rel "stylesheet" :href "/css/style.css"}]
        [:link {:rel "alternate" :type "application/atom+xml" :title c/site-name :href "/feed.xml"}]]
       [:body
        [:div.frame
         (c/banner)
         (c/nav)
         [:div.page
          [:main body]
          (when sidebar (c/sidebar sidebar))]
         [:footer.site-footer
          [:nav.footer-links
           [:a {:href "/"} c/site-name]
           [:span.footer-divider {:aria-hidden "true"}]
           [:a {:href "/tags"} "Tags"]
           [:span.footer-divider {:aria-hidden "true"}]
           [:a {:href "/about"} "About"]
           [:span.footer-divider {:aria-hidden "true"}]
           ;; placeholder address until there's a real inbox
           [:a {:href "mailto:contact@example.com"} "Contact"]]
          [:p.footer-legal
           "Transmissions from Earth Q is an unofficial fan site, not affiliated with or endorsed by DC Comics. "
           "DC characters, titles and related indicia are trademarks of DC Comics. "
           "Cover art and panels are shown for review and commentary. "
           "Reviews and articles © " (c/copyright-years) " Transmissions from Earth Q."]]]]])))
