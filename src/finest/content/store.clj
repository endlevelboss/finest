(ns finest.content.store
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [finest.content.frontmatter :as frontmatter]
            [finest.content.markdown :as markdown]
            [finest.content.post :as post]))

(defonce ^:private state (atom {:posts [] :by-slug {} :loaded-at nil}))
(defonce ^:private known-mtime (atom 0))

(defn- warn
  [msg data]
  (binding [*out* *err*]
    (println "WARN:" msg data)))

(defn- md-file?
  [^java.io.File f]
  (and (.isFile f) (str/ends-with? (.getName f) ".md")))

(def ^:private dir->type
  "Content living under content/posts/ mixes news and review, so its type
   can't be inferred from the directory alone -- everywhere else, the
   directory *is* the type, so an explicit `type:` field in frontmatter
   would just repeat what the folder already says."
  {"collections" "collection"
   "creators"    "creator"
   "lines"       "line"})

(defn- inferred-type
  [^java.io.File f]
  (get dir->type (.getName (.getParentFile f))))

(defn- load-file->post
  [^java.io.File f]
  (let [raw                 (slurp f)
        {:keys [meta body]} (frontmatter/parse raw)
        html                (markdown/render body)
        meta                (update meta :type #(or % (inferred-type f)))]
    (post/->post {:meta          meta
                  :html          html
                  :source-file   (.getName f)
                  :last-modified (.lastModified f)})))

(defn- max-mtime
  [content-dir]
  (reduce max 0 (map #(.lastModified ^java.io.File %) (filter md-file? (file-seq (io/file content-dir))))))

(defn- attach-line-title
  "A collection's own title is just its distinguishing name (\"Year One\");
   the umbrella name (\"Batman\") comes from the line it belongs to. Denormalize
   that here, once, so every listing (cards, tag pages, etc.) can render the
   combined heading without each call site re-resolving the line."
  [by-slug post]
  (if-let [line (and (= :collection (:type post)) (:line post) (get by-slug (:line post)))]
    (assoc post :line-title (:title line))
    post))

(defn load-all!
  "Loads all .md files under content-dir into the in-memory store."
  [content-dir]
  (let [dir        (io/file content-dir)
        raw-posts  (->> (file-seq dir)
                        (filter md-file?)
                        (keep (fn [f]
                                (try
                                  (load-file->post f)
                                  (catch Exception e
                                    (warn (ex-message e) (merge {:file (.getName f)} (ex-data e)))
                                    nil))))
                        (sort-by :date)
                        reverse
                        vec)
        by-slug    (into {} (map (juxt :slug identity)) raw-posts)
        posts      (mapv (partial attach-line-title by-slug) raw-posts)]
    (reset! state {:posts     posts
                    :by-slug   (into {} (map (juxt :slug identity)) posts)
                    :loaded-at (System/currentTimeMillis)})
    (reset! known-mtime (max-mtime content-dir))
    posts))

(defn maybe-reload!
  "Reloads content-dir only if a file has been added/changed/removed since
   the last load. Cheap enough to call on every request in dev."
  [content-dir]
  (when (not= (max-mtime content-dir) @known-mtime)
    (load-all! content-dir)))

(defn all-posts [] (:posts @state))
(defn by-slug [slug] (get (:by-slug @state) slug))
(defn by-tag [tag] (filterv #(contains? (:tags %) tag) (all-posts)))
(defn by-type [type] (filterv #(= type (:type %)) (all-posts)))
(defn all-tags [] (into (sorted-set) (mapcat :tags) (all-posts)))
(defn articles [] (filterv #(not (#{:collection :creator :line} (:type %))) (all-posts)))
(defn referencing [collection-slug] (filterv #(some #{collection-slug} (:collections %)) (all-posts)))
(defn credited-on [creator-slug] (filterv (fn [p] (some #(= creator-slug (:slug %)) (:creators p))) (all-posts)))
(defn under-line
  "Collections in a line, oldest publication first -- a reading order,
   not the reverse-chronological order the rest of the site uses."
  [line-slug]
  (vec (sort-by :date (filterv #(= line-slug (:line %)) (all-posts)))))
