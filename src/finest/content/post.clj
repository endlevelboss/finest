(ns finest.content.post
  (:require [clojure.string :as str])
  (:import [java.time LocalDate ZoneOffset]
           [java.util Date]))

(def ^:private filename-date-prefix
  #"^\d{4}-\d{2}-\d{2}-")

(defn slug-from-filename
  [filename]
  (-> filename
      (str/replace #"\.md$" "")
      (str/replace filename-date-prefix "")))

(defn- ->local-date
  [d]
  (cond
    (instance? LocalDate d) d
    (instance? Date d)      (.. ^Date d toInstant (atZone ZoneOffset/UTC) toLocalDate)
    (string? d)             (LocalDate/parse d)
    :else                   (throw (ex-info "Unrecognized date value" {:date d}))))

(defn ->post
  "Builds and validates a post map from parsed frontmatter, rendered HTML body,
   and file metadata. Throws ex-info on invalid/missing required fields."
  [{:keys [meta html source-file last-modified]}]
  (let [{:keys [title date slug tags type rating cover collections creators]} meta
        post-type (some-> type name keyword)]
    (when-not title
      (throw (ex-info "Post is missing :title" {:source-file source-file})))
    (when-not (#{:news :review :collection :creator} post-type)
      (throw (ex-info "Post :type must be :news, :review, :collection, or :creator"
                       {:source-file source-file :type type})))
    (when (and (not= post-type :creator) (nil? date))
      (throw (ex-info "Post is missing :date" {:source-file source-file})))
    (when (and (= post-type :review) (nil? rating))
      (throw (ex-info "Reviews require a :rating" {:source-file source-file})))
    (cond-> {:slug          (or slug (slug-from-filename source-file))
             :title         title
             :tags          (set tags)
             :type          post-type
             :html          html
             :source-file   source-file
             :last-modified last-modified}
      date                  (assoc :date (->local-date date))
      (= post-type :review) (assoc :rating (double rating))
      cover                 (assoc :cover cover)
      (seq collections)     (assoc :collections (vec collections))
      (seq creators)        (assoc :creators (vec creators)))))
