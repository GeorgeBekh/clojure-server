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
(defn parse-int [s]
  (if (some? s)
    (Long. (re-find  #"\d+" s ))
    nil))


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

(defn group-by-location-id [visits]
  (println "Grouping visits by location id")
  (reduce-kv
   (fn [result id visit]
     (def location-id (get visit "location"))
     (assoc
      result
      location-id (conj (get result location-id) visit)))
   {}
   visits))

(defn filter-visits [visits from-date to-date country to-distance from-age to-age gender]
  (reduce
   (fn [result visit]
     (def date (get visit "visited_at"))
     (def location-id (get visit "location"))
     (def user-id (get visit "user"))
     (def date-condition
       (and
        (if (some? from-date) (> date from-date) true)
        (if (some? to-date) (< date to-date) true)))
     (def country-condition
       (if (some? country)
         (=
          (get-in locations [location-id "country"])
          country)
         true))
     (def distance-condition
       (if (some? to-distance)
         (<
          (get-in locations [location-id "distance"])
          to-distance)
         true))
     (def age-condition
       (and
        (if (some? from-age) (> (get-in users [user-id "age"]) from-age) true)
        (if (some? to-age) (< (get-in users [user-id "age"]) to-age) true)))
     (def gender-condition
       (if (some? gender)
         (=
          (get-in users [user-id "gender"])
          gender)
         true))
     (if (and date-condition country-condition distance-condition age-condition gender-condition)
       (conj
        result
        visit)
       result))
   '()
   visits))

(defn calculate-avg-mark [visits]
  (def result
    (reduce
     #(hash-map :sum (+ (:sum %) (get %2 "mark")) :count (inc (:count %)))
     {:sum 0 :count 0}
     visits))
  (if (= (:count result) 0)
    0
    (/ (:sum result) (:count result))))

(def visits-by-user-id (group-by-user-id visits))
(def visits-by-location-id (group-by-location-id visits))

(def entities {:users users :visits visits :locations locations})

(defroutes app-routes
  (GET "/" [])
  (GET ["/:entity/:id", :id #"[0-9]+"] [entity id]
       (def result (get (get entities (keyword entity)) (read-string id)))
       (and result { :body result}))
  (GET ["/users/:id/visits", :id #"[0-9]+"] [id fromDate toDate country toDistance]
       (let
           [id (parse-int id)
            from-date (parse-int fromDate)
            to-date (parse-int toDate)
            to-distance (parse-int toDistance)]
         (def result
           (filter-visits (get visits-by-user-id id) from-date to-date country to-distance nil nil nil)))
       (and result {:body {:visits result}}))
  (GET ["/locations/:id/avg", :id #"[0-9]+"] [id fromDate toDate fromAge toAge gender]
       (let
           [id (parse-int id)
            from-date (parse-int fromDate)
            to-date (parse-int toDate)
            from-age (parse-int fromAge)
            to-age (parse-int toAge)]
         (def result
           (calculate-avg-mark (filter-visits (get visits-by-location-id id) from-date to-date nil nil from-age to-age gender))))
       (and result {:body result}))
;;  (POST ["/user/:id", :id #"[0-9]+"] [id email first_name last_name birth_date]
;;        (if (not (and email first_name last_name birth_date))
;;          {:status 400}
;;          ()))
;;        )
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults api-defaults)
      (middlewares/wrap-json)))
