(ns finest.routes
  (:require [reitit.ring :as ring]
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

(defn app
  []
  (ring/ring-handler
    (router)
    (ring/routes
      (ring/create-resource-handler {:path "/"})
      (ring/create-default-handler {:not-found h/not-found}))))
