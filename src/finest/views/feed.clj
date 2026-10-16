(ns finest.views.feed
  "The Atom feed at /feed.xml. Feed readers need absolute URLs everywhere,
   so every link -- including the root-relative ones inside post bodies --
   is built from the site's public base URL."
  (:require [clojure.string :as str]
            [hiccup2.core :as h]
            [finest.views.components :as c])
  (:import (java.time LocalDate ZoneOffset)
           (java.time.format DateTimeFormatter)))

(def ^:private timestamp-formatter
  (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssXXX"))

(defn- timestamp
  "Posts are dated to the day, but Atom wants a full timestamp -- midnight
   UTC stands in for the time of day."
  [^LocalDate date]
  (.format (.atStartOfDay date ZoneOffset/UTC) timestamp-formatter))

(defn absolutize-links
  "Rewrites root-relative href/src attributes (\"/images/x.jpg\") to full
   URLs on `base-url`, so images and links still work inside a reader.
   Protocol-relative (\"//host/...\") and already-absolute URLs are left alone."
  [html base-url]
  (str/replace html #"\b(href|src)=\"/(?!/)"
               (fn [[_ attr-name]] (str attr-name "=\"" base-url "/"))))

(defn- entry
  [base-url {:keys [slug date tags html] :as post}]
  (let [url (str base-url "/posts/" slug)]
    [:entry
     [:title (c/display-title post)]
     [:link {:rel "alternate" :type "text/html" :href url}]
     [:id url]
     [:published (timestamp date)]
     [:updated (timestamp date)]
     (for [tag (sort tags)]
       [:category {:term tag}])
     [:content {:type "html"} (absolutize-links html base-url)]]))

(defn feed
  "`posts` newest first, each with its embeds already expanded into :html."
  [{:keys [base-url posts]}]
  (str
    (h/html {:mode :xml}
      (h/raw "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
      [:feed {:xmlns "http://www.w3.org/2005/Atom"}
       [:title c/site-name]
       [:link {:rel "self" :type "application/atom+xml" :href (str base-url "/feed.xml")}]
       [:link {:rel "alternate" :type "text/html" :href (str base-url "/")}]
       [:id (str base-url "/")]
       [:updated (timestamp (or (:date (first posts)) (LocalDate/now ZoneOffset/UTC)))]
       [:author [:name c/site-name]]
       (map (partial entry base-url) posts)])))
