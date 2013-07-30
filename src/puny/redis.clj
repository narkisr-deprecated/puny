(ns puny.redis
  (:require 
    [flatland.useful.map :refer (map-vals)]
    [clojure.set :refer (difference)]
    [taoensso.timbre :refer [debug info error warn trace ]]
    [taoensso.carmine :as car]))

; to be binded by users
(declare server-conn)

(def ^:private conn (atom {}))

(defn server-conn [p] (reset! conn p))

(defn read-conn 
   []
   (deref conn)
  )

(defmacro wcar [& body]
   `(try
       (car/wcar (read-conn) ~@body)
       (catch Exception e#
         (error e#)
         )))

(defn clear-all [] (wcar (car/flushdb)))

(defn missing-keys [rk m]
  (difference (into #{} (map keyword (car/hkeys rk))) (into #{} (keys m))))

(defn hsetall* [rk m & [missing]]
  "The reverse action of hgetall*, missing keys will be removed (if provided)."
    (when missing (wcar (doseq [d missing] (car/hdel rk d))))
    (apply car/hmset rk (flatten (into [] (map-vals m car/freeze)))) )

