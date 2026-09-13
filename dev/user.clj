(ns user
  (:require [clojure.tools.namespace.repl :as tn-repl]
            [finest.routes :as routes]
            [finest.server :as server]
            [finest.content.store :as store]))

(tn-repl/set-refresh-dirs "src" "dev")

(def content-dir "content")

(defn- wrap-dev-content-reload
  [handler]
  (fn [request]
    (store/maybe-reload! content-dir)
    (handler request)))

(defn go
  []
  (store/load-all! content-dir)
  (server/start! {:handler (wrap-dev-content-reload (routes/app)) :port 3000})
  :ready)

(defn stop
  []
  (server/stop!))

(defn reset
  []
  (stop)
  (tn-repl/refresh :after 'user/go))

(defn reload-content!
  []
  (store/load-all! content-dir))
