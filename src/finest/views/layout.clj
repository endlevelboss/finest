(ns finest.views.layout
  (:require [hiccup2.core :as h]
            [finest.views.components :as c]))

(defn page
  [{:keys [title body]}]
  (str
    (h/html
      (h/raw "<!DOCTYPE html>\n")
      [:html {:lang "en"}
       [:head
        [:meta {:charset "utf-8"}]
        [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
        [:title (if title (str title " — Longbox Blue") "Longbox Blue")]
        [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin ""}]
        [:link {:rel "stylesheet"
                :href "https://fonts.googleapis.com/css2?family=Anton&family=Archivo:wght@400;600;700&family=Source+Serif+4:ital,wght@0,400;0,600;1,400&display=swap"}]
        [:link {:rel "stylesheet" :href "/css/style.css"}]]
       [:body
        (c/nav)
        [:main body]
        [:footer.site-footer
         [:span.footer-brand "Longbox Blue"]
         [:a {:href "/tags"} "Tags"]]]])))
