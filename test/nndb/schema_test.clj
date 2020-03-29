(ns nndb.schema-test
  (:require [clojure.test :refer :all]

            [com.stuartsierra.component :as component]

            [nndb.db :as db]
            [nndb.schema :refer :all]
            [nndb.system :refer [new-system]]))

(defonce system nil)

(defn warmup
  [tests-func]
  (let [sys (new-system false)]
    (do
      (alter-var-root #'system (constantly (component/start sys)))
      (tests-func)
      (component/stop system)
      (alter-var-root #'system (constantly nil)))))

(defn q
  [& args]
  (apply execute-query system args))

; TODO use spec
(defn valid?
  [res]
  (and (nil? (:errors res)) 
       (or (map? (:data res))
           (sequential? (:data res)))
       (< 0 (count (:data res)))))

(use-fixtures :once warmup)

(deftest test-network-optimizer-resolver
  (is (valid? (q "{network(name: \"zhixuhao/unet/unet\") {optimizer {type hasDecay learningRate}}}"))))

(deftest test-network-losses-resolver
  (is (valid? (q "{network(name: \"zhixuhao/unet/unet\") {losses {type}}}"))))

(deftest test-network-query
  (is (valid? (q "{network (name: \"foo\") {name} }")))
  (is (valid? (q "{network (name: \"zhixuhao/unet/unet\") {name}}")))
  (is (valid? (q "{network (name: \"zhixuhao/unet/unet\") {name numLayers numInputs numOutputs}}"))))

(deftest test-networks-query
  (testing "Getting network properties"
    (is (valid? (q "{networks(network: {minNumInputs: 2}) {
                       name
                       numLayers
                       numInputs
                       numOutputs}}"))))
  (testing "All filters"
    (is (valid? (q "{networks(network: {minNumLayers: 10} 
                       optimizers: {type: \"Adam\"}
                       losses: {type: \"binary_crossentropy\"}
                       layers: {type: \"Conv2D\"}) {
                           name
                           }}"))))
  (testing "Input dims"
    (is (valid? (q "{networks(layers: {minInputDim: 2}) { name }}"))))
  (testing "Multiple layer filters"
    (is (valid? (q "{networks(layers: [{type: \"Conv2D\"}, {type: \"Conv1D\"}]) {
                       name
                       }}")))))
