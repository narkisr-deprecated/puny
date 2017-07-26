(ns puny.test.migration
  "puny migration support"
  (:require
   [taoensso.carmine :as car]
   [puny.redis :as r :refer (clear-all wcar)]
   [puny.core :as p])
  (:import clojure.lang.ExceptionInfo)
  (:use midje.sweet))

(r/server-conn {:pool {} :spec {:host "localhost"}})

(def current-version 2)

(declare update-car)

(defn upgrade-car [f & args]
  (let [res (apply f args) version (-> res meta :version)]
    (if (and (map? res) (or (nil? version) (> current-version version)))
      (do (update-car (assoc res :tires "big")) (apply f args)) res)))

(with-state-changes [(before :facts (clear-all))]
  (fact "basic upgrade" :integration :puny
        (p/entity {:version current-version} car :id license :indices [color] :intercept {:read [upgrade-car]})
        (defn validate-car [car] {})
        (add-car {:license 123 :color "black"}) => 123
        (wcar (car/hset (car-id 123) :meta {:version 1}))
        (get-car 123) => {:license 123 :color "black" :tires "big"}
        (:version (meta (get-car 123))) => 2
        (update-car {:license 123 :color "black" :tires "small"})
        (get-car 123) => {:license 123 :color "black" :tires "small"}
        (car-exists? 123) => truthy
        (add-car {:license 124 :color "black" :tires "huge!"}) => 124
        (get-car-index :color "black") => ["123" "124"]
        (get-car 124) => {:license 124 :color "black" :tires "huge!"}))
