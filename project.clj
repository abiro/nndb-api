(defproject nndb "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license 
  {:name "GNU AGPLv3"
   :url "https://www.gnu.org/licenses/agpl-3.0.txt"}
  :aliases 
  {"all-tests" ["do" "check," "eastwood," "kibit," "cloverage," "test"]
   }
  :main nndb.system
  :target-path "target/%s"
  :jvm-opts ["-Dconfig.edn=resources/config.edn"]
  :uberjar-name "standalone.jar"
  :profiles
  {:uberjar
   {:aot :all
    }
   :dev
   {:main user
    :jvm-opts ["-Dconfig.edn=dev-resources/config.edn"]
    :plugins [[jonase/eastwood "0.2.9"]
              [lein-kibit "0.1.6"]
              [lein-cloverage "1.0.13"]]
    }
   }
  :dependencies 
  ; Until the PR to lacinia-pedestal is accepted, it has to be built manually:
  ; 1. Check out git@github.com:abiro/lacinia-pedestal.git
  ; 2. In the repo directory: `lein do pom, jar, install`
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/data.json "0.2.6"]

   [com.outpace/config "0.12.0"]
   [com.stuartsierra/component "0.3.2"]
   [com.walmartlabs/lacinia-pedestal "0.10.0"]
   [org.slf4j/slf4j-simple "1.7.25"]
   [org.apache.tinkerpop/gremlin-core "3.3.3"]
   [org.apache.tinkerpop/tinkergraph-gremlin "3.3.3"]
   [clojurewerkz/ogre "3.3.2.0"]
   ])
