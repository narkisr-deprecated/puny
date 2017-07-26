(ns puny.test.redis
  (:require
   [taoensso.carmine :as car]
   [puny.redis :as r :refer (wcar hsetall* missing-keys)])
  (:use midje.sweet))

(fact "hsetall sanity" :integration :redis
      (wcar (car/del "play"))
      (wcar (hsetall* "play" {:one {:two {:three 1}}})) => "OK"
      (wcar (car/hgetall* "play" true)) => {:one {:two {:three 1}}}
      (wcar (hsetall* "play" {:one {:six {:seven 3} :four {:five 2}}})) => "OK"
      (wcar (car/hgetall* "play" true)) => {:one {:six {:seven 3} :four {:five 2}}}
      (let [missing (wcar (missing-keys "play" {:two {:three 2}}))]
        (wcar (hsetall* "play" {:two {:three 2}} missing))) => "OK"
      (wcar (car/hgetall* "play" true)) =>  {:two {:three 2}})
