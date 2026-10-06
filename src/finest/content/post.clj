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

(defn upcoming?
  "Whether a collection's release date is still after `today`, so pages can
   say \"Releases\" instead of \"Released\" for collections that haven't
   shipped yet. Asked at render time rather than stored on the post, so a
   long-running server flips it on release day without a reload."
  [{:keys [release-date]} ^LocalDate today]
  (boolean (and release-date (.isAfter ^LocalDate release-date today))))

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

(defn issue-key
  "What two volumes' double-dip entries are matched on: the issue name,
   ignoring case and spacing, so \"Catwoman (vol 2) #14\" and
   \"catwoman (vol 2)  # 14\" are the same issue."
  [issue]
  (-> (str issue)
      str/lower-case
      (str/replace #"\s+" " ")
      (str/replace #"\s*#\s*" "#")
      str/trim))

(defn- normalize-double-dip
  "A double-dip entry -- an issue (or part of one) this volume shares with
   another: :issue plus whatever optional fields were given -- :part (which
   bit of the issue this volume reprints, e.g. \"Green Arrow backup\"),
   :date (freeform display text), :creators (credits for just this issue)."
  [{:keys [issue part date creators]}]
  (cond-> {:issue (str issue)}
    part           (assoc :part part)
    date           (assoc :date date)
    (seq creators) (assoc :creators (mapv normalize-creator creators))))

(def ^:private feature-img-tag
  "An <img> tag marked as a post's featured image via its markdown title:
   ![Batman](/images/batman.jpg \"feature\")."
  #"<img\b[^>]*\btitle=\"feature\"[^>]*>")

(defn- attr
  [tag attr-name]
  (second (re-find (re-pattern (str "\\b" attr-name "=\"([^\"]*)\"")) tag)))

(defn- extract-feature-image
  "The first image in a post body marked \"feature\", as {:src :alt}, for
   listing cards to reuse -- nil when nothing is marked."
  [html]
  (when-let [tag (and html (re-find feature-img-tag html))]
    {:src (attr tag "src") :alt (attr tag "alt")}))

(defn- mark-feature-image
  "Swaps the first marked image's title=\"feature\" for a class, so the
   article doesn't show a \"feature\" tooltip on hover."
  [html]
  (str/replace-first html feature-img-tag
                     #(str/replace-first % "title=\"feature\"" "class=\"feature-image\"")))

(defn ->post
  "Builds and validates a post map from parsed frontmatter, rendered HTML body,
   and file metadata. Throws ex-info on invalid/missing required fields."
  [{:keys [meta html source-file last-modified]}]
  (let [{:keys [title date slug tags type rating cover collections creators line double-dips release-date]} meta
        post-type      (some-> type name keyword)
        freeform-date? (= post-type :collection)
        sort-d         (when date (sort-date freeform-date? date))
        feature-image  (extract-feature-image html)]
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
             :html          (if feature-image (mark-feature-image html) html)
             :source-file   source-file
             :last-modified last-modified}
      date                  (assoc :date-display (display-date date))
      sort-d                (assoc :date sort-d)
      release-date          (assoc :release-date (->local-date release-date)
                                    :release-date-display (display-release-date release-date))
      (= post-type :review) (assoc :rating (double rating))
      cover                 (assoc :cover cover)
      feature-image         (assoc :feature-image feature-image)
      (seq collections)     (assoc :collections (vec collections))
      (seq creators)        (assoc :creators (mapv normalize-creator creators))
      (seq double-dips)     (assoc :double-dips (mapv normalize-double-dip double-dips))
      line                  (assoc :line line))))
