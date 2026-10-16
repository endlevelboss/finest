(ns finest.views.meta
  "The page description and preview image that go in each page's head,
   for search results and link previews (Open Graph tags)."
  (:require [clojure.string :as str]))

(def ^:private max-length
  "Search engines show roughly this much before cutting the text off."
  155)

(def ^:private named-entities
  {"amp" "&" "lt" "<" "gt" ">" "quot" "\"" "apos" "'" "nbsp" " "})

(defn- decode-entity
  "&amp; &apos; &#39; &#x27; and the like back to their characters;
   anything unrecognized is left as written."
  [[match nm dec hex]]
  (cond
    nm  (get named-entities nm match)
    dec (str (char (parse-long dec)))
    hex (str (char (Long/parseLong hex 16)))))

(defn- plain-text
  "An HTML fragment's text: tags dropped, entities decoded, whitespace
   collapsed. Hiccup escapes it again when it's written out."
  [html]
  (-> html
      (str/replace #"<[^>]*>" "")
      (str/replace #"&(?:([a-z]+)|#(\d{1,5})|#x([0-9a-fA-F]{1,4}));" decode-entity)
      (str/replace #"\s+" " ")
      str/trim))

(defn- shorten
  "Cuts text to max-length at a word boundary, marking the cut with …"
  [text]
  (if (<= (count text) max-length)
    text
    (let [cut (subs text 0 max-length)
          cut (subs cut 0 (or (str/last-index-of cut " ") max-length))]
      (str (str/replace cut #"[\s,;:.–—-]+$" "") "…"))))

(defn excerpt
  "The first paragraph of a rendered body that has any text in it --
   paragraphs holding only an image are skipped -- shortened for a
   description. Nil when the body has no text paragraph at all."
  [html]
  (some->> (re-seq #"(?s)<p\b[^>]*>(.*?)</p>" (or html ""))
           (map (comp plain-text second))
           (remove str/blank?)
           first
           shorten))

(defn absolute-url
  "Root-relative paths (\"/images/x.jpg\") joined to `base-url`; full
   URLs are left alone."
  [base-url path]
  (if (and (str/starts-with? path "/") (not (str/starts-with? path "//")))
    (str base-url path)
    path))
