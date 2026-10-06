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

(defn- attach-review-of
  "The volumes a review covers, resolved from its `collections:` slugs so
   every listing can show \"A review of: ...\" without a lookup. Keeps the
   frontmatter order, drops slugs that aren't loaded collections, and runs
   after attach-line-title so each volume carries its line's name (and
   slug, so the kicker can link the line too)."
  [by-slug post]
  (let [volumes (when (= :review (:type post))
                  (->> (:collections post)
                       (keep by-slug)
                       (filter #(= :collection (:type %)))
                       (mapv #(select-keys % [:slug :title :line :line-title]))))]
    (if (seq volumes)
      (assoc post :review-of volumes)
      post)))

(defn- creators-of
  "A post's full creator credits: the cover creators: plus any credited on
   just one double-dipped issue -- they still count."
  [post]
  (concat (:creators post) (mapcat :creators (:double-dips post))))

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

(defn- double-dip-fragments
  "Every (collection, double-dip) pair across the site, grouped by the
   issue it reprints (post/issue-key). Each fragment carries just enough
   about its parent collection to render a link and a combined heading
   (shaped to match components/display-title's input) plus which part of
   the issue it has."
  [posts]
  (->> posts
       (filter #(= :collection (:type %)))
       (mapcat (fn [c]
                 (for [{:keys [issue part]} (:double-dips c)]
                   {:key        (post/issue-key issue)
                    :collection (cond-> {:slug (:slug c) :title (:title c)}
                                  (:line-title c) (assoc :line-title (:line-title c))
                                  part            (assoc :part part))})))
       (group-by :key)))

(defn- attach-double-dip-siblings
  "For each double-dip, assoc :siblings -- the other volumes reprinting
   the same issue (or another part of it), so a reader can hop between
   them. Entries no other volume shares yet are left untouched."
  [fragments-by-key post]
  (if (and (= :collection (:type post)) (seq (:double-dips post)))
    (update post :double-dips
            (fn [dips]
              (mapv (fn [{:keys [issue] :as dip}]
                      (let [siblings (->> (get fragments-by-key (post/issue-key issue))
                                          (map :collection)
                                          (remove #(= (:slug %) (:slug post)))
                                          vec)]
                        (cond-> dip (seq siblings) (assoc :siblings siblings))))
                    dips)))
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
        line-titled      (into {} (map (juxt :slug identity)) with-line-titles)
        with-review-of   (mapv (partial attach-review-of line-titled) with-line-titles)
        fragments        (double-dip-fragments with-review-of)
        with-siblings    (mapv (partial attach-double-dip-siblings fragments) with-review-of)
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
(defn review-for
  "The most recent review referencing this collection, if any -- all-posts
   is already newest-first, and referencing preserves that order."
  [collection-slug]
  (first (filter #(= :review (:type %)) (referencing collection-slug))))
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


(defn releases-around
  "Splits dated collections around `today`: the `n` most recent already
   out and the `n` next to come (today counts as out). Both lists run in
   release order, oldest first, so the two read as one timeline."
  [collections today n]
  (let [dated (filter :release-date collections)
        out?  #(not (post/upcoming? % today))]
    {:recent   (vec (reverse (take n (sort-by :release-date #(compare %2 %1) (filter out? dated)))))
     :upcoming (vec (take n (sort-by :release-date (remove out? dated))))}))

(defn release-window
  [today n]
  (releases-around (by-type :collection) today n))
