(ns clojure-server.handler
  (:require [compojure.core :refer :all]
            [clojure-server.loader]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


(def testing clojure-server.loader/testing)

(defroutes app-routes
  (GET "/" [])
  (GET "/true" [])
  (GET "/false" [])
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
