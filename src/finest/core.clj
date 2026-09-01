(ns finest.core
  (:require [finest.server :as server]
            [finest.routes :as routes]
            [finest.content.store :as store])
  (:gen-class))

(defn -main
  [& _args]
  (let [port        (Integer/parseInt (or (System/getenv "PORT") "3000"))
        content-dir (or (System/getenv "CONTENT_DIR") "content/posts")]
    (store/load-all! content-dir)
    (server/start! {:handler (routes/app) :port port :join? true})))
