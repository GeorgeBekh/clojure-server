(ns clojure-server.loader)

(defn get-filename [name num]
  (str "./data/" name "_" num ".json"))

(defn get-content
  ([name callback]
   (get-content name callback 1))
  ([name callback num]
   (def filename (get-filename name num))
   (if (.exists (clojure.java.io/file filename))
     (do
       (callback (slurp filename))
       (get-content name callback (+ num 1))))))

(get-content "users" (fn [content] (println (subs content 0 10))))

(def testing "test")
