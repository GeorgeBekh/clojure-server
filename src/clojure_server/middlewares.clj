(ns clojure-server.middlewares
  (:require [cheshire.core :as json]))

(defn wrap-json [handler]
  (fn [request]
    (let [response (handler request)]
      (println (type (get response :body)))
      (conj
       response
       (assoc-in response [:headers "Content-type"] "application/json;charset=UTF-8")
       (hash-map :body (json/generate-string (get response :body)))))))
