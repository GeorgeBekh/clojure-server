(ns clojure-server.handler
  (:require [compojure.core :refer :all]
            [clojure-server.loader :as loader]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure-server.middlewares :as middlewares]))

(def users (loader/load "users"))
(def visits (loader/load "visits"))
(def locations (loader/load "locations"))

(def entities {:users users :visits visits :locations locations})

(defroutes app-routes
  (GET "/" [])
  (GET ["/:entity/:id", :id #"[0-9]+"] [entity id]
       (def result (get (get entities (keyword entity)) (read-string id)))
       (when-not (= result nil)
         (hash-map :body result)))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      (middlewares/wrap-json)))
