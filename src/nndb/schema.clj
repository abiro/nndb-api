; Based on the Lacinia GraphQL framework tutorial
; https://lacinia.readthedocs.io/en/latest/tutorial/
; Code license for the tutorial: https://github.com/walmartlabs/clojure-game-geek/blob/e0d360fb1b3191c15a844cadcbd4583484d28c0a/LICENSE

(ns nndb.schema
  "GraphQL schema resolvers and the full schema."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.walk :as walk]

            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            [com.walmartlabs.lacinia.util :as lutil]
            [com.stuartsierra.component :as component]

            [nndb.config :as config]
            [nndb.db]
            [nndb.graph])
  (:import (clojure.lang IPersistentMap)))

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and 
   eliminates ordering problems."
  [m]
  (walk/postwalk
    (fn [node]
      (cond
        (instance? IPersistentMap node) (into {} node)
        (seq? node) (vec node)
        :else node))
    m))

(defn execute-query
  "Execute a query. Useful for testing."
  [system query-string]
  (-> system
      :schema-provider
      :schema
      (lacinia/execute query-string nil nil)
      simplify))

(defn query-graph
  "Query the graph using a function from nndb.graph namespace."
  [func-name db & args]
    (apply (ns-resolve 'nndb.graph func-name) (:graph db) args))

(defn get-network
  [db]
  (fn [_ args _]
    (query-graph 'get-network-by-name db (:name args))))

(defn get-networks
  [db]
  (fn [_ args _]
    (query-graph 'get-networks db args)))

(defn get-optimizer
  [db]
  (fn [_ args network]
    (query-graph 'get-network-optimizer db (:uuid network))))

(defn get-losses
  [db]
  (fn [_ args network]
    (query-graph 'get-network-losses db (:uuid network))))

(defn resolver-map
  "GraphQL schema resolvers."
  [component]
  (let [db (:db component)]
    {:Network/optimizer (get-optimizer db)
     :Network/losses (get-losses db)
     :query/network (get-network db)
     :query/networks (get-networks db)}))

(defn load-schema
  "Load the GraphQL schema from the resources."
  [component]
  (-> (io/resource config/schema-resource)
      slurp
      edn/read-string
      (lutil/attach-resolvers (resolver-map component))
      schema/compile))

(defrecord SchemaProvider [schema]

  component/Lifecycle

  (start [this]
    (assoc this :schema (load-schema this)))

  (stop [this]
    (assoc this :schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:db]))})
