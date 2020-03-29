; Based on the Lacinia GraphQL framework tutorial
; https://lacinia.readthedocs.io/en/latest/tutorial/
; Code license for the tutorial: https://github.com/walmartlabs/clojure-game-geek/blob/e0d360fb1b3191c15a844cadcbd4583484d28c0a/LICENSE

(ns nndb.server
  (:require [com.stuartsierra.component :as component]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]

            [nndb.config :as config]))

(defrecord Server [server]

  component/Lifecycle

  (start [this]
    (assoc this :server (-> this
                            :schema-provider
                            :schema
                            (lp/service-map {:port config/port
                                             :graphiql config/start-graphiql})
                            http/create-server
                            http/start)))

  (stop [this]
    (http/stop server)
    (assoc this :server nil)))

(defn new-server
  []
  {:server (-> {}
               map->Server
               (component/using [:schema-provider]))
   })
