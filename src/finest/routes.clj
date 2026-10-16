(ns finest.routes
  (:require [reitit.ring :as ring]
            [ring.middleware.head :refer [wrap-head]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.util.response :as response]
            [finest.handler :as h]))

(defn router
  []
  (ring/router
    [["/" {:get h/index}]
     ["/reviews" {:get h/reviews-list}]
     ["/articles" {:get h/articles-list}]
     ["/collections" {:get h/collections-list}]
     ["/creators" {:get h/creators-list}]
     ["/lines" {:get h/lines-list}]
     ["/tags" {:get h/tag-index}]
     ["/tags/:tag" {:get h/tag-list}]
     ["/posts/:slug" {:get h/post-page}]
     ["/feed.xml" {:get h/feed}]
     ["/about" {:get h/about}]
     ["/sitemap.xml" {:get h/sitemap}]
     ["/robots.txt" {:get h/robots}]]))

(def ^:private static-max-age
  "How long browsers may reuse CSS, images and icons without asking again:
   a day. They rarely change, and when they do, visitors pick the change
   up within a day (or at once, with a hard refresh). After that they
   only re-check, and an unchanged file costs a tiny 304 response."
  (* 24 60 60))

(defn- wrap-static-caching
  [handler]
  (fn [request]
    (some-> (handler request)
            (response/header "Cache-Control" (str "public, max-age=" static-max-age)))))

(defn app
  "Static-file caching is off unless `:cache-static?` is set, as the
   production server does, so CSS edits show up at once in dev."
  ([] (app {}))
  ([{:keys [cache-static?]}]
   (-> (ring/ring-handler
         (router)
         (ring/routes
           (cond-> (ring/create-resource-handler {:path "/"})
             cache-static? wrap-static-caching)
           (ring/create-default-handler {:not-found h/not-found})))
       wrap-not-modified
       ;; answer HEAD (link checkers, uptime monitors) like GET, minus the body
       wrap-head)))
