(ns clojure-server.middlewares
  (:require [cheshire.core :as json]))

(defn wrap-json [handler]
  (fn [request]
    (let [response (handler request)]
      (conj
       (assoc-in response [:headers "Content-Type"] "application/json;charset=UTF-8")
       (hash-map :body (json/generate-string (:body response)))))))
