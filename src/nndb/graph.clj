(ns nndb.graph
  (:require [clojure.walk :as walk]
            [clojure.repl]
            [clojure.string :refer [starts-with?]]

            [clojurewerkz.ogre.core :as og]

            [nndb.util :as util])

  (:import (java.util NoSuchElementException)
           (org.apache.tinkerpop.gremlin.structure Property T Vertex VertexProperty$Cardinality)
           (org.apache.tinkerpop.gremlin.process.traversal P Traverser)))

(defmacro into-seq!
  [graph & steps]
  `(-> ~graph 
       og/traversal
       ~@steps 
       og/into-seq!))

(defmacro iterate!
  [graph & steps]
  `(-> ~graph 
       og/traversal
       ~@steps 
       og/iterate!))

(defmacro to-map!
  [graph & steps]
  `(try
     (-> ~graph 
         og/traversal
         ~@steps
         og/value-map
         og/next!
         normalize-property-map)
     (catch NoSuchElementException e#
       nil)))

(defn exec-steps
  "Execute multiple traversal steps.

  `traversal` should be a Gremlin traversal.
  `steps` should be a sequence of functions that take a traversal as argument
  and return a traversal.

  Returns a Gremlin travesal.
  "
  [traversal steps]
  ((apply comp (reverse steps)) traversal))

(defn normalize-property-map
  "Normalize the property map returned by a Gremlin traversal"
  [m]
  (-> m
      util/into-map
      util/to-single-cardinality
      walk/keywordize-keys))

(defn normalize-property-maps
  "Normalize a sequence of property maps returned by a Gremlin traversal"
  [prop-maps]
  (map normalize-property-map prop-maps))

(defn get-vertex-props
  "Get the properties of a vertex as a seq."
  [vertex]
  (-> vertex
      (dissoc :label)
      walk/stringify-keys
      seq))

(defn add-vertex-prop-fn
  "Create a function that adds a vertex property."
  [prop]
  (fn [traversal]
    (apply og/property traversal VertexProperty$Cardinality/single prop)))

(defn get-vertex-prop-steps
  [vertex]
  (map add-vertex-prop-fn (get-vertex-props vertex)))

(defn add-vertex
  "Add a vertex to the graph.
  Vertex must have `label` and `uuid` properties.
  "
  [graph vertex]
  (iterate! graph
            (og/addV (:label vertex))
            (exec-steps (get-vertex-prop-steps vertex))))

(defn add-edge
  "Add an edge to the graph.

  Edge must have `label`, `fromUUID` and `toUUID` properties."
  [graph edge]
  ; TODO add spec verification
  (iterate! graph

            og/V
            (og/has :uuid (:fromUUID edge))
            (og/as :from)

            og/V
            (og/has :uuid (:toUUID edge))
            (og/as :to)

            (og/add-E (:label edge))
            (og/from :from)
            (og/to :to)))

(defn get-all-vertices
  "Get all vertices in the graph"
  [graph]
  (into-seq! graph
             og/V
             (og/value-map true)))

(defn get-all-edges
  "Get all edges in the graph"
  [graph]
  (into-seq! graph
             og/E
             (og/value-map true)))

(defn get-network-by-name
  "Get a network by name."
  [graph network-name]
  (to-map! graph
           og/V
           (og/has :name network-name)))

(defn get-network-optimizer
  "Get the optimizer of a network."
  [graph network-uuid]
  (to-map! graph
           og/V
           (og/has :uuid network-uuid)
           (og/in :OptimizerOf)))

(defn get-network-losses
  "Get the loss functions of a network."
  [graph network-uuid]
  (normalize-property-maps
    (into-seq! graph
               og/V
               (og/has :uuid network-uuid)
               (og/in :LossOf)
               og/value-map)))

(defn get-property-predicate
  "Get the property predicate from an input key."
  [arg-key]
  (let [arg-name (name arg-key)]
    (cond (starts-with? arg-name "min") #(P/gte %)
          (starts-with? arg-name "max") #(P/lte %)
          (starts-with? arg-name "has") nil
          :else #(P/eq %))))

(defn get-property-key
  "Get the property key from an input key."
  [arg-key]
  (let [arg-name (name arg-key)
        prefix (subs arg-name 0 (min 3 (count arg-name)))]
    (keyword
      (cond (contains? #{"min" "max" "has"} prefix)
            (util/decapitalize (subs arg-name 3))

            :else
            arg-name))))

(defn has-step
  "Graph traversal step to filter by property."
  [field value predicate traversal]
  (if (nil? predicate)
    (og/has traversal field)
    (og/has traversal field (predicate value))))

(defn get-property-filters
  "Generate a sequence of has property filters from a map of properties."
  [properties]
  (for [[k value] properties
        :let [pred (get-property-predicate k)
              field (get-property-key k)]
        :when pred]
    (partial has-step field value pred)))

(defn get-disjunct-property-filters
  "Generate a disjunction step from a sequence of property maps."
  [traversal properties-seq]
  ; `__` step is needed to pass in the current traversal.
  ; `identity` step is needed because `__` macro rewrites the first argument as
  ; anonymus traversal starting step.
  (apply og/or traversal (map #(og/__ (og/identity)
                                      (exec-steps (get-property-filters %)))
                              properties-seq)))

(defn get-inbound-property-filters
  "Filter vertices by vertex properties of inbound vertices."
  ([traversal edge-label properties-map]
   (if (some? properties-map)
     (og/where traversal (og/__ (og/in edge-label)
                                (exec-steps
                                  (get-property-filters properties-map))))
     traversal))
  ([traversal bool-op edge-label properties-seq]
   (if (some? properties-seq)
     (apply bool-op traversal (map #(og/__ (og/identity)
                                           (get-inbound-property-filters
                                             edge-label
                                             %))
                                   properties-seq))
     traversal)))

(defn print-traversal
  "Debugging utility."
  [t]
  (println t)
  t)

(defn get-networks
  "Get networks filtered by network properties, layers, losses or optimizers."
  [graph args]
  (let [network (:network args)
        optimizers (:optimizers args)
        losses (:losses args)
        layers (:layers args)]
    (normalize-property-maps
      (into-seq! graph
                 og/V
                 (og/has-label :Network)
                 (exec-steps (get-property-filters network))
                 (get-inbound-property-filters og/or :OptimizerOf optimizers)
                 (get-inbound-property-filters og/or :LossOf losses)
                 (get-inbound-property-filters og/and :LayerOf layers)
                 og/value-map))))

(defn get-prop-counts
  "Get value counts of a vertices' properties."
  [graph vertex-type property]
  (into-seq! graph
             og/V
             (og/has :type vertex-type)
             (og/values property)
             og/group-count))

