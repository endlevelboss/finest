(ns finest.content.markdown
  (:require [markdown.core :as md]))

(defn render
  "Renders a Markdown body string to an HTML string."
  [body]
  (md/md-to-html-string body))
