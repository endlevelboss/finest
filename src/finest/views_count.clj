(ns finest.views-count
  "Counts page views of /posts/<slug> -- reviews, articles, volumes,
   lines and creators alike -- per day, and keeps them in an EDN file:

     {\"2026-11-03\" {\"silver-age-gl\" 14, \"green-lantern-defeat-green-lantern\" 6}}

   Only a count per page per day: no IP addresses, no cookies. Bots that
   say so in their user agent aren't counted. Counts live in memory and
   are written to the file once a minute, and when the server shuts
   down. Only the production server (finest.core) counts; dev doesn't."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (java.io File)
           (java.nio.file Files StandardCopyOption)
           (java.time LocalDate)
           (java.util.concurrent Executors ThreadFactory TimeUnit)))

(defonce ^:private counts (atom {}))
(defonce ^:private dirty? (atom false))

(def ^:private bot-pattern
  "Crawlers, link-preview fetchers (Discordbot, Slackbot,
   facebookexternalhit, Bluesky's Cardyb), feed readers, uptime monitors
   and command-line tools all tend to name themselves like this."
  #"(?i)bot|crawl|spider|slurp|preview|facebookexternalhit|cardyb|embedly|feed|monitor|curl|wget|python|java/|go-http|okhttp|headless")

(defn bot?
  "True for a user agent that looks automated -- or for none at all,
   which browsers always send."
  [user-agent]
  (or (nil? user-agent) (boolean (re-find bot-pattern user-agent))))

(defn- viewed-slug
  "The slug of a page view worth counting: a successful GET of
   /posts/<slug> from something that isn't a bot. Nil otherwise -- HEAD
   requests, 404s (a mistyped slug, a draft on the live server) and
   redirects don't count."
  [{:keys [request-method uri headers]} {:keys [status]}]
  (when (and (= :get request-method)
             (= 200 status)
             (not (bot? (get headers "user-agent"))))
    (second (re-matches #"/posts/([a-z0-9-]+)" uri))))

(defn record!
  [day slug]
  (swap! counts update-in [(str day) slug] (fnil inc 0))
  (reset! dirty? true))

(defn wrap-count-views
  "Wrap outside wrap-head, which turns HEAD into GET before the request
   reaches anything inside it."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (when-let [slug (viewed-slug request response)]
        (record! (LocalDate/now) slug))
      response)))

(defn- read-file
  [^File f]
  (if (.exists f)
    (edn/read-string (slurp f))
    {}))

(defn flush!
  "Writes the counts to `f` if anything changed since the last write --
   to a temporary file first, then moved over the old one, so a crash
   mid-write never leaves half a file behind."
  [^File f]
  (when (compare-and-set! dirty? true false)
    (let [tmp (File. (str (.getPath f) ".tmp"))]
      (io/make-parents f)
      (spit tmp (pr-str (into (sorted-map) @counts)))
      (Files/move (.toPath tmp) (.toPath f)
                  (into-array [StandardCopyOption/REPLACE_EXISTING StandardCopyOption/ATOMIC_MOVE])))))

(defn start!
  "Picks up the counts already in `path` (so restarts keep adding to
   them) and writes back once a minute and on shutdown."
  [path]
  (let [f (io/file path)]
    (reset! counts (read-file f))
    (reset! dirty? false)
    (doto (Executors/newSingleThreadScheduledExecutor
           (reify ThreadFactory
             (newThread [_ r] (doto (Thread. r "views-count") (.setDaemon true)))))
      (.scheduleWithFixedDelay #(try (flush! f)
                                     (catch Exception e
                                       (binding [*out* *err*]
                                         (println "WARN: Couldn't write view counts" (ex-message e)))))
                               1 1 TimeUnit/MINUTES))
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(flush! f)))))

(defn top
  "The `n` most viewed slugs in a counts map, as [slug views] pairs --
   over every day in it, or only days on or after `since` (\"2026-11-01\").
   For a quick look at the REPL: (top (edn/read-string (slurp \"views.edn\")) 20)."
  ([counts n] (top counts n nil))
  ([counts n since]
   (->> counts
        (filter (fn [[day _]] (or (nil? since) (>= (compare day since) 0))))
        (map val)
        (apply merge-with +)
        (sort-by val >)
        (take n))))
