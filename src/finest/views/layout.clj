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
        [:title (if title (str title " — Comic Blog") "Comic Blog")]
        [:link {:rel "stylesheet" :href "/css/style.css"}]]
       [:body
        (c/nav)
        [:main body]
        [:footer.site-footer
         [:span "Comic Blog"]
         [:a {:href "/tags"} "Tags"]]]])))
