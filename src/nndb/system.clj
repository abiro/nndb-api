; Based on the Lacinia GraphQL framework tutorial
; https://lacinia.readthedocs.io/en/latest/tutorial/
; Code license for the tutorial: https://github.com/walmartlabs/clojure-game-geek/blob/e0d360fb1b3191c15a844cadcbd4583484d28c0a/LICENSE

(ns nndb.system
  (:require
    [com.stuartsierra.component :as component]

    [nndb.db :as db]
    [nndb.schema :as schema]
    [nndb.server :as server])
  (:gen-class))

(defn new-system
  ([]
   (new-system true))
  ([include-server]
   (merge (component/system-map)
          (db/new-db)
          (schema/new-schema-provider)
          (when include-server (server/new-server)))))

(defn start-system
  []
  (component/start (new-system)))

(defn stop-system
  [system]
  (component/stop system))

(defn -main
  [& args]
  (start-system))
