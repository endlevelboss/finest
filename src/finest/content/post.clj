(ns finest.content.post
  (:require [clojure.string :as str])
  (:import [java.time LocalDate ZoneOffset]
           [java.time.format DateTimeFormatter]
           [java.util Date]))

(def ^:private filename-date-prefix
  #"^\d{4}-\d{2}-\d{2}-")

(defn slug-from-filename
  [filename]
  (-> filename
      (str/replace #"\.md$" "")
      (str/replace filename-date-prefix "")))

(defn humanize-slug
  "\"bill-finger\" -> \"Bill Finger\" -- used to give an auto-generated
   page a readable title when nothing better is available."
  [slug]
  (->> (str/split slug #"-")
       (map str/capitalize)
       (str/join " ")))

(defn- ->local-date
  [d]
  (cond
    (instance? LocalDate d) d
    (instance? Date d)      (.. ^Date d toInstant (atZone ZoneOffset/UTC) toLocalDate)
    (string? d)             (LocalDate/parse d)
    :else                   (throw (ex-info "Unrecognized date value" {:date d}))))

(defn- real-date?
  [d]
  (or (instance? LocalDate d) (instance? Date d)))

(defn- display-date
  "Human-facing date string. Real date/timestamp values are normalized to
   ISO form; anything else (plain strings, bare-year YAML longs) is shown
   as authored -- this is how collections get freeform ranges like
   \"1986-1987\" or \"Jan 1990 - Aug 1990\"."
  [d]
  (if (real-date? d)
    (str (->local-date d))
    (str d)))

(def ^:private release-date-formatter
  (DateTimeFormatter/ofPattern "MMM d, yyyy"))

(defn- display-release-date
  "A collection's street date is always a real date -- no freeform ranges --
   so it's always shown in the same human-friendly form, e.g. \"Nov 5, 2024\"."
  [d]
  (.format (->local-date d) release-date-formatter))

(defn- sort-date
  "Best-effort chronological sort key. Collections may only be dated to a
   year or a range, so we fall back to the first 4-digit year found in the
   display string, anchored to Jan 1 -- good enough for ordering, not for
   display."
  [freeform? d]
  (cond
    (nil? d)       nil
    (real-date? d) (->local-date d)
    freeform?      (when-let [y (re-find #"\d{4}" (str d))]
                      (LocalDate/of (Integer/parseInt y) 1 1))
    :else          (->local-date d)))

(defn- normalize-creator
  "Accepts either a bare slug string (creators: [alan-moore, dave-gibbons])
   or a {:slug :role} map with role optional -- always returns a map, so
   downstream code only has one shape to deal with."
  [c]
  (if (map? c)
    (select-keys c [:slug :role])
    {:slug c}))

(defn- normalize-issue
  "An issue entry: :number plus whatever optional fields were given --
   :date (freeform display text), :creators (overrides the collection's
   cover credits for just this issue), :original (a shared id linking this
   issue to its other fragments across different collections/lines)."
  [{:keys [number date original creators]}]
  (cond-> {:number number}
    date           (assoc :date date)
    original       (assoc :original original)
    (seq creators) (assoc :creators (mapv normalize-creator creators))))

(defn ->post
  "Builds and validates a post map from parsed frontmatter, rendered HTML body,
   and file metadata. Throws ex-info on invalid/missing required fields."
  [{:keys [meta html source-file last-modified]}]
  (let [{:keys [title date slug tags type rating cover collections creators line issues release-date]} meta
        post-type      (some-> type name keyword)
        freeform-date? (= post-type :collection)
        sort-d         (when date (sort-date freeform-date? date))]
    (when-not title
      (throw (ex-info "Post is missing :title" {:source-file source-file})))
    (when-not (#{:news :review :collection :creator :line} post-type)
      (throw (ex-info "Post :type must be :news, :review, :collection, :creator, or :line"
                       {:source-file source-file :type type})))
    (when (and (not (#{:creator :line} post-type)) (nil? date))
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
      date                  (assoc :date-display (display-date date))
      sort-d                (assoc :date sort-d)
      release-date          (assoc :release-date (->local-date release-date)
                                    :release-date-display (display-release-date release-date))
      (= post-type :review) (assoc :rating (double rating))
      cover                 (assoc :cover cover)
      (seq collections)     (assoc :collections (vec collections))
      (seq creators)        (assoc :creators (mapv normalize-creator creators))
      (seq issues)          (assoc :issues (mapv normalize-issue issues))
      line                  (assoc :line line))))
