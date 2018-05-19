(ns clojure-server.handler
  (:require [compojure.core :refer :all]
            [clojure-server.loader :as loader]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [clojure-server.middlewares :as middlewares]))

(def users (loader/load "users"))
(def visits (loader/load "visits"))
(def locations (loader/load "locations"))

;;(defn get-by-user-id [visits user-id]
;;  "Get visits by user id without any indexing"
;;  (reduce-kv
;;   (fn [grouped id visit]
;;     (if (= (get visit "user") user-id)
;;       (conj grouped visit)
;;       grouped))
;;   (list)
;;   visits))

(defn group-by-user-id [visits]
  "Group visits by user-id to speed up reads"
  (println "Grouping visits by user id")
  (reduce-kv
   (fn [result id visit]
     (def user-id (get visit "user"))
     (assoc
      result
      user-id (conj (get result user-id) visit)))
   {}
   visits))

(defn filter-visits [visits from-date to-date country to-distance]
  (reduce
   (fn [result visit]
     (def date (get visit "visited_at"))
     (def location-id (get visit "location"))
     (def date-condition
       (and
        (if (some? from-date) (> date from-date) true)
        (if (some? to-date) (< date to-date) true)))
     (def country-condition
       (if (some? country)
         (=
          (get-in [location-id "country"] locations)
          country)
         true))
     (def distance-condition
       (if (some? to-distance)
         (<
          (get-in [location-id "distance"] locations)
          to-distance)
         true))
     (when (and date-condition country-condition distance-condition)
       (conj
        result
        visit)))
   '()
   visits))

(def visits-by-user-id (group-by-user-id visits))

(def entities {:users users :visits visits :locations locations})

(defroutes app-routes
  (GET "/" [])
  (GET ["/:entity/:id", :id #"[0-9]+"] [entity id]
       (def result (get (get entities (keyword entity)) (read-string id)))
       (when-not (= result nil)
         (hash-map :body result)))
  (GET ["/users/:id/visits", :id #"[0-9]+"] [id]
       (def result (filter-visits (get visits-by-user-id (read-string id)) nil nil nil nil))
;;       (def result (get-by-user-id visits (read-string id))) ;;slower read
       (when-not (= result nil)
         {:body {:visits result}}))
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (middlewares/wrap-json)))
