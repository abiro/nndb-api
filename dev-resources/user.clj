; Based on the Lacinia GraphQL framework tutorial
; https://lacinia.readthedocs.io/en/latest/tutorial/
; Code license for the tutorial: https://github.com/walmartlabs/clojure-game-geek/blob/e0d360fb1b3191c15a844cadcbd4583484d28c0a/LICENSE

(ns user
  (:require
    [clojure.edn :as edn]
    [clojure.java.browse :refer [browse-url]]
    [clojure.java.io :as io]
    [clojure.test :refer [run-tests]]

    [com.stuartsierra.component :as component]

    [nndb.config :as config]
    [nndb.schema :as schema]
    [nndb.system :as system]))


(defonce system nil)

(defn q 
  [query-string]
  (schema/execute-query system query-string))

(defn load-schema
  "Used to get better error messages on loading the schema."
  []
  (-> (io/resource config/schema-resource)
      slurp
      edn/read-string))

(defn start
  ([]
   (start false))
  ([start-server]
   (alter-var-root #'system (fn [_]
                              (-> (system/new-system start-server)
                                  component/start-system)))
   (when start-server (browse-url (str "http://localhost:" config/port)))
   :started))

(defn stop
  []
  (when (some? system)
    (component/stop-system system)
    (alter-var-root #'system (constantly nil)))
  :stopped)

(defn reload-user
  []
  (stop)
  (require 'user :reload-all))

(def name-query "{ network_by_name(name: \"zhixuhao/unet/unet\") { name } }")

(comment
  (start)
  (stop)

  (def vertex1 {:label "Layer" :uuid "1"})
  (def vertex2 {:label "Layer" :uuid "2"})
  (def vertex3 {:label "Layer" :uuid "3"})
  (def vertices [vertex1 vertex2 vertex3])

  (def edge1 {:label "Tensor" :fromUUID "1" :toUUID "2"})
  (def edge2 {:label "Tensor" :fromUUID "2" :toUUID "3"})
  (def edges [edge1 edge2])
  )
