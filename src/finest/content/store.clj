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
  "Walks up from f's parent looking for a directory named collections/
   creators/lines, so files can be filed into alphabetical subfolders
   (e.g. content/collections/b/batman-year-one.md) without losing type
   inference. Stops at root, exclusive, so files sitting directly in
   content/ (e.g. content/posts/*) still get no inferred type."
  [^java.io.File f ^java.io.File root]
  (loop [dir (.getParentFile f)]
    (when (and dir (not= dir root))
      (or (get dir->type (.getName dir))
          (recur (.getParentFile dir))))))

(defn- load-file->post
  [^java.io.File f ^java.io.File root]
  (let [raw                 (slurp f)
        {:keys [meta body]} (frontmatter/parse raw)
        html                (markdown/render body)
        meta                (update meta :type #(or % (inferred-type f root)))]
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

(defn- creators-of
  "A post's full creator credits: the cover creators: plus any per-issue
   overrides -- a creator only credited on one issue still counts."
  [post]
  (concat (:creators post) (mapcat :creators (:issues post))))

(defn- credited-creator-slugs
  [posts]
  (into #{} (comp (filter #(= :collection (:type %))) (mapcat creators-of) (map :slug)) posts))

(defn- generated-creator
  "A stand-in page for a creator credited on a collection but with no
   creator file of their own yet -- title is guessed from the slug, and
   the page still lists everything they're credited on."
  [slug]
  {:slug        slug
   :title       (post/humanize-slug slug)
   :type        :creator
   :tags        #{}
   :html        "<p><em>No profile written yet.</em></p>"
   :generated?  true})

(defn- issue-fragments
  "Every (collection, issue) pair across the site that has an :original id,
   grouped by that id. Each fragment carries just enough about its parent
   collection to render a link and a combined heading (shaped to match
   components/display-title's input) plus its own issue number. Carrying
   :line (not just :line-title) lets a collection tell whether a sibling
   belongs to a genuinely different line, worth surfacing as related."
  [posts]
  (->> posts
       (filter #(= :collection (:type %)))
       (mapcat (fn [c]
                 (for [issue (:issues c) :when (:original issue)]
                   {:original   (:original issue)
                    :collection (cond-> {:slug (:slug c) :title (:title c)}
                                  (:line-title c) (assoc :line-title (:line-title c))
                                  (:line c)       (assoc :line (:line c)))
                    :number     (:number issue)})))
       (group-by :original)))

(defn- attach-issue-siblings
  "For each issue with an :original id, assoc :siblings -- the other
   fragments (in other collections) sharing that id, so a reader can hop
   between them to reconstruct the original historical issue. Issues with
   no :original, or no siblings, are left untouched."
  [fragments-by-original post]
  (if (= :collection (:type post))
    (update post :issues
            (fn [issues]
              (mapv (fn [issue]
                      (if-let [orig (:original issue)]
                        (let [siblings (->> (get fragments-by-original orig)
                                             (remove #(= (:slug (:collection %)) (:slug post)))
                                             (mapv #(assoc (:collection %) :number (:number %))))]
                          (cond-> issue (seq siblings) (assoc :siblings siblings)))
                        issue))
                    issues)))
    post))

(defn load-all!
  "Loads all .md files under content-dir into the in-memory store."
  [content-dir]
  (let [dir              (io/file content-dir)
        raw-posts        (->> (file-seq dir)
                              (filter md-file?)
                              (keep (fn [f]
                                      (try
                                        (load-file->post f dir)
                                        (catch Exception e
                                          (warn (ex-message e) (merge {:file (.getName f)} (ex-data e)))
                                          nil))))
                              (sort-by :date)
                              reverse
                              vec)
        by-slug-raw      (into {} (map (juxt :slug identity)) raw-posts)
        with-line-titles (mapv (partial attach-line-title by-slug-raw) raw-posts)
        fragments        (issue-fragments with-line-titles)
        with-siblings    (mapv (partial attach-issue-siblings fragments) with-line-titles)
        missing-creators (remove by-slug-raw (credited-creator-slugs with-siblings))
        posts            (into with-siblings (map generated-creator) missing-creators)]
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
(defn credited-on [creator-slug] (filterv (fn [p] (some #(= creator-slug (:slug %)) (creators-of p))) (all-posts)))
(defn- by-date-undated-last
  "Ascending by content date, oldest first -- collections with no
   extractable year (a freeform \"TBA\" or similar) sort after every
   dated one, rather than `sort-by`'s default of treating nil as the
   smallest value and sorting them first."
  [a b]
  (let [da (:date a)
        db (:date b)]
    (cond
      (and da db) (compare da db)
      da          -1
      db          1
      :else       0)))

(defn under-line
  "Collections in a line, oldest publication first -- a reading order,
   not the reverse-chronological order the rest of the site uses."
  [line-slug]
  (vec (sort by-date-undated-last (filterv #(= line-slug (:line %)) (all-posts)))))

(defn line-related
  "Every review/news post referencing any collection filed under this
   line -- so a review of one specific volume still surfaces on the
   line's own page, not just that volume's. Newest first, like every
   other reverse-chronological listing on the site."
  [line-slug]
  (->> (under-line line-slug)
       (mapcat #(referencing (:slug %)))
       distinct
       (sort-by :date)
       reverse
       vec))
