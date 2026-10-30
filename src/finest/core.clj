(ns finest.core
  (:require [finest.server :as server]
            [finest.routes :as routes]
            [finest.content.store :as store]
            [finest.views-count :as views-count])
  (:gen-class))

(defn -main
  [& _args]
  (let [port        (Integer/parseInt (or (System/getenv "PORT") "3000"))
        content-dir (or (System/getenv "CONTENT_DIR") "content")
        views-file  (or (System/getenv "VIEWS_FILE") "data/views.edn")]
    (store/load-all! content-dir)
    (views-count/start! views-file)
    (server/start! {:handler (views-count/wrap-count-views (routes/app {:cache-static? true}))
                    :port port :join? true})))
