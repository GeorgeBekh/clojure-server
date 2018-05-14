(ns clojure-server.handler
  (:require [compojure.core :refer :all]
            [clojure-server.loader :as loader]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def users (loader/load "users"))
(def visits (loader/load "visits"))
(def locations (loader/load "locations"))

(defroutes app-routes
  (GET "/" [])
  (GET "/true" [])
  (GET "/false" [])
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
