; Configs keys are names in this namespaces.
(ns nndb.config
  (:require 
    [com.stuartsierra.component :as component]
    [outpace.config :refer [defconfig defconfig!]]))

(defconfig
  ^{:validate [string? "Must be string."]}
  graph-dir
  "dev-resources/graph")

(defconfig
  ^{:validate [int? "Must be integer."]}
  port
  8000)

(defconfig
  ^{:validate [string? "Must be string."]}
  schema-resource
  "schema.edn")

(defconfig 
  ^{:validate [boolean? "Must be boolean."]}
  start-graphiql 
  false)
