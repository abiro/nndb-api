(ns nndb.db
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.ogre.core :as og]

            [nndb.config :as config]
            [nndb.graph :as g]
            [nndb.util :as util])

  (:import (org.apache.tinkerpop.gremlin.structure Graph T Vertex)
           (org.apache.tinkerpop.gremlin.tinkergraph.structure TinkerGraph)))

(defn make-tinker-graph 
  "Make a TinkerGraph graph instance."
  []
  (og/open-graph {(Graph/GRAPH) (.getName TinkerGraph)}))

(defn create-indices
  "Create indices for graph"
  [graph]
  (.createIndex ^TinkerGraph graph "uuid" Vertex)
  (.createIndex ^TinkerGraph graph "type" Vertex)
  (.createIndex ^TinkerGraph graph "networkName" Vertex))

(defn import-graph
  "Import a graph from jsonl vertex and edge files."
  ([]
   (import-graph config/graph-dir))
  ([in-dir]
   (import-graph (util/join-path in-dir "vertices.jsonl")
                 (util/join-path in-dir "edges.jsonl")))
  ([vertices-path edges-path]
   (import-graph (make-tinker-graph) vertices-path edges-path))
  ([graph vertices-path edges-path]
   (do 
     (println "Creating indices")
    ; TODO speciy indices in jsonl file.
     (create-indices graph)
     (println "Processing:" vertices-path)
     (util/process-file-by-lines vertices-path
                                 util/read-json-kw
                                 (partial g/add-vertex graph))
     (println "Processing:" edges-path)
     (util/process-file-by-lines edges-path 
                                 util/read-json-kw
                                 (partial g/add-edge graph))
     (println "Created graph")
     graph)))

(defrecord GraphDB [graph]
  component/Lifecycle
  (start [this]
    (assoc this :graph (import-graph)))
  (stop [this]
    (assoc this :graph nil)))

(defn new-db
  []
  {:db (map->GraphDB {})
   })

