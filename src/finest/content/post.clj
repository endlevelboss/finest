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
  (let [{:keys [title date slug tags type rating]} meta]
    (when-not title
      (throw (ex-info "Post is missing :title" {:source-file source-file})))
    (when-not date
      (throw (ex-info "Post is missing :date" {:source-file source-file})))
    (let [post-type (some-> type name keyword)]
      (when-not (#{:news :review} post-type)
        (throw (ex-info "Post :type must be :news or :review"
                         {:source-file source-file :type type})))
      (when (and (= post-type :review) (nil? rating))
        (throw (ex-info "Reviews require a :rating" {:source-file source-file})))
      (cond-> {:slug          (or slug (slug-from-filename source-file))
               :title         title
               :date          (->local-date date)
               :tags          (set tags)
               :type          post-type
               :html          html
               :source-file   source-file
               :last-modified last-modified}
        (= post-type :review) (assoc :rating (double rating))))))
