(ns nndb.util
  (:require [clojure.data.json :as json]
            [clojure.string :as string]

            [outpace.config])
  (:import (java.io File)))

(defn dissoc-except
  "Remove all keys from a map except for the provided ones."
  [m & ks]
  (let [except (set ks)]
    (apply dissoc m (filter #(not (contains? except %)) (keys m)))))

(defn decapitalize
  "Convert first character of a string to lower case."
  [s]
  (apply str (string/lower-case (first s)) (rest s)))

(defn into-map
  "Turn a collection into a map."
  [coll]
  (into {} coll))

(defn java-list?
  "Test whether an object implements the java.util.List interface."
  [obj]
  (instance? java.util.List obj))

(defn to-single-cardinality
  "Produce a new map by taking the first value of every sequential value in a 
  map."
  [m]
  (into {}
        (for [[k v] m]
          [k (if ((some-fn java-list? sequential?) v) (first v) v)])))

(defn read-json-kw
  "Read a json and convert key strings to keywords."
  [json-str]
  (json/read-str json-str :key-fn keyword))

(defn join-path
  "Join file paths."
  [& paths]
  (clojure.string/join File/separator paths))

(defn optional-to-int
  "Parse an optional config from the com.outpace/config library to int."
  [optional-val]
  (if (outpace.config/provided? optional-val)
    (-> optional-val
        outpace.config/extract
        Integer/parseInt)
    optional-val))

(defn process-file-by-lines
  "Process file reading it line-by-line.
  From: https://stackoverflow.com/a/25950711/2650622"
  ([file]
   (process-file-by-lines file identity))
  ([file process-fn]
   (process-file-by-lines file process-fn println))
  ([file process-fn output-fn]
   (with-open [rdr (clojure.java.io/reader file)]
     (doseq [line (line-seq rdr)]
       (output-fn
         (process-fn line))))))
