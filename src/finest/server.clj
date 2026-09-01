(ns finest.server
  (:require [ring.adapter.jetty :as jetty]))

(defonce ^:private instance (atom nil))

(defn start!
  [{:keys [handler port join?] :or {port 3000 join? false}}]
  (when @instance
    (throw (ex-info "Server already running" {})))
  (reset! instance (jetty/run-jetty handler {:port port :join? join?}))
  @instance)

(defn stop!
  []
  (when-let [server @instance]
    (.stop server)
    (reset! instance nil)))
