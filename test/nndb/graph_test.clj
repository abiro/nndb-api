(ns nndb.graph-test
  (:require [clojure.test :refer :all]

            [com.stuartsierra.component :as component]

            [nndb.db :as db]
            [nndb.util :as util]
            [nndb.graph :refer :all]
            [nndb.system :refer [new-system]])
  (:import (org.apache.tinkerpop.gremlin.structure VertexProperty$Cardinality)))

(defonce graph nil)

(defmacro using-graph
  [func-name & args]
  `(~func-name graph ~@args))

(defn start-db 
  [tests-func]
  (let [system (component/start (new-system false))
        graph (-> system :db :graph)]
    (do
      (alter-var-root #'graph (constantly graph))
      (tests-func)
      (component/stop system)
      (alter-var-root #'graph (constantly nil)))))

; TODO use spec instead.
(defn valid-map?
  [m]
  (and (map? m)
       (every? keyword? (keys m))
       (every? (some-fn number? string? boolean?) (vals m))))

(use-fixtures :once start-db)

(deftest test-get-property-key
  (is (= (get-property-key :minNumLayers) :numLayers))
  (is (= (get-property-key :foo) :foo)))

(deftest test-get-network-optimizer
  (let [network-name "basveeling/wavenet/wavenet"
        network-data (using-graph get-network-by-name network-name)
        uuid (:uuid network-data)
        res (using-graph get-network-optimizer uuid)]
    (is (valid-map? res))))

(deftest test-get-network-losses
  (let [network-name "basveeling/wavenet/wavenet"
        network-data (using-graph get-network-by-name network-name)
        uuid (:uuid network-data)
        res (using-graph get-network-losses uuid)]
    (is (every? valid-map? res))))

(deftest test-get-network-by-name
  (testing "Network doesn't exist"
    (is (nil? (using-graph get-network-by-name nil))))
  (testing "Existing network"
    (let [network-name "basveeling/wavenet/wavenet"
          res (using-graph get-network-by-name network-name)]
      (is (= (:name res) network-name))
      (is (valid-map? res)))))

(deftest test-get-networks
  (let [params {:network {:minNumLayers 10}
                :optimizers [{:type "Adam"}]
                :losses [{:type "binary_crossentropy"}]
                :layers [{:type "Conv2D"}]}
        res-none (using-graph get-networks {})
        res-net (using-graph get-networks
                             (util/dissoc-except params :network))
        res-opts (using-graph get-networks
                             (util/dissoc-except params :optimizers))
        res-2-opts (using-graph get-networks
                                {:optimizerss [{:type "Adam"} {:type "SGD"}]})
        res-losses (using-graph get-networks
                                (util/dissoc-except params :losses))
        res-2-losses (using-graph get-networks
                                  {:losses [{:type "binary_crossentropy"}
                                            {:type "categorical_crossentropy"}]})
        res-layers (using-graph get-networks
                                (util/dissoc-except params :layers))
        res-2-layers (using-graph get-networks
                                  {:layers [{:type "Conv2D"}
                                            {:type "MaxPooling2D"}]})
        res-input-dim-high (using-graph get-networks
                                        {:layers [{:minInputDim 100}]})
        res-net-opt (using-graph get-networks
                                 (util/dissoc-except params
                                                     :network
                                                     :layers))
        res-layers-opt (using-graph get-networks
                                    (util/dissoc-except params
                                                        :optimizers
                                                        :layers))
        res-3 (using-graph get-networks
                           (util/dissoc-except params
                                               :network
                                               :optimizers
                                               :layers))
        res-all (using-graph get-networks params)]
    (testing "Results have correct shape"
      (is (every? valid-map? res-none))
      (is (every? valid-map? res-net))
      (is (every? valid-map? res-opts))
      (is (every? valid-map? res-2-opts))
      (is (every? valid-map? res-losses))
      (is (every? valid-map? res-2-losses))
      (is (every? valid-map? res-layers))
      (is (every? valid-map? res-2-layers))
      (is (every? valid-map? res-net-opt))
      (is (every? valid-map? res-layers-opt))
      (is (every? valid-map? res-3))
      (is (every? valid-map? res-all))
      )
    (testing "Expected number of results are returned"
      (is (< 0 (count res-none)))
      (is (< 0 (count res-net) (count res-none)))
      (is (< 0 (count res-opts) (count res-none)))
      (is (< 0 (count res-opts) (count res-2-opts)))
      (is (< 0 (count res-losses) (count res-none)))
      (is (< 0 (count res-losses) (count res-2-losses)))
      (is (< 0 (count res-layers) (count res-none)))
      (is (< 0 (count res-2-layers) (count res-layers)))
      (is (= 0 (count res-input-dim-high)))
      (is (< 0 (count res-net-opt) (count res-net)))
      (is (< 0 (count res-layers-opt) (count res-opts)))
      (is (< 0 (count res-3) (count res-net-opt)))
      (is (< 0 (count res-all) (count res-3)))
    )))

(deftest test-get-all-vertices
  (testing "No vertices in the graph."
      (is (nil? (get-all-vertices (db/make-tinker-graph)))))
  (testing "Get all vertices from a non-empty graph."
    (let [res (using-graph get-all-vertices)]
      (is (seq? res))
      (is (not-empty res)))))
