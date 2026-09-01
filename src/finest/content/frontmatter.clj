(ns finest.content.frontmatter
  (:require [clj-yaml.core :as yaml]))

(def ^:private fm-pattern
  #"(?s)\A---\r?\n(.*?)\r?\n---\r?\n?(.*)\z")

(defn parse
  "Splits raw file contents into {:meta {...} :body \"...\"}.
   Throws ex-info if frontmatter is missing or malformed."
  [raw-text]
  (if-let [[_ yaml-block body] (re-matches fm-pattern raw-text)]
    {:meta (into {} (yaml/parse-string yaml-block :keywords true))
     :body body}
    (throw (ex-info "Missing or malformed frontmatter"
                     {:snippet (subs raw-text 0 (min 80 (count raw-text)))}))))
