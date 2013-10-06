(ns puny.test.interception
  "Testing puny interception api"
 (:require 
    [puny.redis :as r :refer (clear-all wcar)]  
    [puny.core :as p])
 (:use midje.sweet))

(r/server-conn {:pool {} :spec {:host "localhost"}})

(defn map-plus-1 [m]
  (into {} (map (fn [[k v]] [k (+ 1 v)]) m)))

(defn create-fn [f & args]
  (if (map? (first args))
    (f (map-plus-1 (first args))) (apply f args)))

(defn read-fn 
  [f & args] 
   (let [res (apply f args)]
     (if (map? res) (map-plus-1 res) res)))

(defn update-fn 
  [f & args] 
   (if (map? (first args)) 
     (f (first args) (map-plus-1 (second args))) (apply f args)))

(with-state-changes [(before :facts (clear-all))]
 (fact "Create interception" :integration :interception
     (p/entity crt :intercept {:create create-fn})        
     (defn validate-crt [w] {})
     (let [id (add-crt {:bar 1})]
          (get-crt id) => {:bar 2}
          (crt-exists? id) => truthy
          (update-crt id {:bar 3}) 
          (get-crt id) => {:bar 3}
          (delete-crt id)
          (crt-exists? id) => falsey
       )) 

 (fact "Read interception" :integration :interception
     (p/entity rd :intercept {:read read-fn})      
     (defn validate-rd [w] {})
     (let [id (add-rd {:bar 1})]
          (get-rd id) => {:bar 2}
          (rd-exists? id) => truthy
          (update-rd id {:bar 3}) 
          (get-rd id) => {:bar 4}
          (delete-rd id)
          (rd-exists? id) => falsey))

 #_(fact "Update interception" :integration :interception
     (p/entity up :intercept {:update update-fn})      
     (defn validate-up [w] {})
     (let [id (add-up {:bar 1})]
          (get-up id) => {:bar 1}
          (up-exists? id) => truthy
          (update-up id {:bar 2}) 
          ;; (partial-up id {:bar 2}) 
          (get-up id) => {:bar 3}
          (delete-up id)
          (up-exists? id) => falsey))
  ) 
