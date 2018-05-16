(ns clojure-server.loader
  (:require [cheshire.core :refer :all]))

(defn get-filename [name num]
  (str "./data/" name "_" num ".json"))

(defn get-content [name]
  (loop [i 1 collection '()]
    (def filename (get-filename name i))
    (if (.exists (clojure.java.io/file filename))
      (do
        (println (str "Reading file " filename))
        (def entities (list (get (parse-string (slurp filename)) name)))
        (recur
         (inc i)
         (concat collection entities)))
      collection)))

(defn convert [content]
  (reduce
   (fn [a entities]
     (merge a (reduce
      (fn [a entity]
        (assoc a (get entity "id") entity))
      (hash-map)
      entities)))
   (hash-map)
   content))

(defn load [entity-name]
   (convert (get-content entity-name)))
