(comment 
   Puny , Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns puny.core
  "A tiny redis based ORM,
   Caveat: crud+index operations are not atomic, introducing lua procedures will fix that."
  (:use
    [taoensso.timbre :only (debug info error warn trace)]
    [clojure.set :only (difference)]
    [slingshot.slingshot :only  [throw+ try+]]
    [puny.redis :only (wcar hsetall*)]
    [robert.hooke :only (add-hook)]
    [clojure.core.strint :only (<<)])
  (:require 
    [puny.ids :refer (<<< fn-ids id-modifiers indices-fn-ids ex bang-fn-ids)]
    [clojure.string :refer [split]]
    [taoensso.carmine :as car]))

(defmacro defgen 
  "An id generator" 
  [name*]
  `(defn ~(<<< "gen-~{name*}") []
    (wcar (car/incr ~(<< "~{name*}:ids")))))

(defn index-key 
  "index key format, composed of name index type and value: person:namejohn"
   [name* t v]
  (str name* t v))

(defmacro index-fns
  "Create index functions (enabled if there are indices defined)."
  [name* {:keys [indices]}]
  (let [{:keys [index-add index-del index-get reindex]} (indices-fn-ids name*)
        indices-ks (into [] (map keyword indices))] 
    `(do 
       (defn- ~index-add [~'id ~'v]
         (doseq [i# ~indices-ks]
           (car/sadd (index-key '~name* i# (get ~'v i#)) ~'id )))

       (defn ~index-get [~'k ~'v]
         (wcar (car/smembers (index-key '~name* ~'k ~'v))))

       (defn- ~index-del [~'id ~'v]
         (doseq [i# ~indices-ks]
           (car/srem (index-key '~name* i# (get ~'v i#)) ~'id)))

        (defn- ~reindex [~'id ~'old ~'new]
          (~index-del ~'id ~'old) 
          (~index-add ~'id ~'new)))))

(defmacro bang-fns
  "A fail fast versions of read/delete functions (will fail if entity is missing), 
  the 'u' part functions are '!' by default (meaning they always fails fast)." 
  [name*]
  (let [{:keys [id-fn delete-fn get-fn exists-fn]} (fn-ids name*)
        {:keys [missing-ex exists! delete! get!]} (bang-fn-ids name*) ]

    `(do 
       (defn ~exists! [~'id]
         (when-not (~exists-fn ~'id)
           (throw+ {:type ~missing-ex} ~(<< "Missing ~{name*}")))
         true)

       (defn ~delete!  [~'id] 
         (when (some #(= 0 %) (flatten (vector (~delete-fn ~'id))))
            (throw+ {:type ~missing-ex} ~(<< "Missing ~{name*}"))))

       (defn ~get! [~'id] 
         (let [r# (~get-fn ~'id)]
           (when (empty? r#) 
             (throw+ {:type ~missing-ex} ~(<< "Missing ~{name*}")))
            r# 
           )))))

(defmacro write-fns 
  "Creates the add/update functions both take into account if id is generated of provided"
  [name* opts meta*]
  (let [{:keys [id-fn validate-fn add-fn update-fn gen-fn get-fn partial-fn exists-fn]} (fn-ids name*)
        {:keys [missing-ex]} (bang-fn-ids name*) opts-m (apply hash-map opts)
        {:keys [up-args up-id add-k-fn]} (id-modifiers name* opts-m)
        {:keys [index-add index-del reindex]} (indices-fn-ids name*)
        {:keys [exists!]} (bang-fn-ids name*)]
    `(do 
       (declare ~validate-fn)

       (defn- ~gen-fn []
         "generated ids for entities"
         (wcar (~id-fn (car/incr ~(<< "~{name*}:ids")))))

       (defn ~add-fn [~'v]
         (~validate-fn ~'v)
         (let [id# ~add-k-fn]
           (when (~exists-fn id#) 
             (throw+ {:type ~(ex name* "conflicting")} ~(<< "Adding existing ~{name*}")))
           (wcar 
             (~index-add id# ~'v)  
             (hsetall* (~id-fn id#) (assoc ~'v :meta ~meta*))) 
           id#))

       (defn ~partial-fn ~up-args
         (~exists! ~up-id)
         (let [orig# (wcar (car/hgetall* (~id-fn ~up-id) true)) updated# (merge-with merge orig# ~'v)]
           (wcar 
             (~reindex ~up-id orig# updated#)
             (hsetall* (~id-fn ~up-id) (assoc updated# :meta ~meta*)))))

       (defn ~update-fn ~up-args
         (~validate-fn ~'v)
         (~exists! ~up-id)
         (let [orig# (wcar (car/hgetall* (~id-fn ~up-id) true)) 
               missing# (difference (into #{} (keys orig#)) (into #{} (keys ~'v)))]
           (wcar 
             (~reindex ~up-id orig# ~'v) 
             (hsetall* (~id-fn ~up-id) (assoc ~'v :meta ~meta*) missing#)))))))


(defn- hooks [name* {:keys [create read update delete]}] 
   (let [{:keys [add-fn update-fn partial-fn delete-fn]} (fn-ids name*)
         {:keys [get-fn exists-fn all-fn]} (fn-ids name*)
         {:keys [exists! get! delete!]} (bang-fn-ids name*) 
         {:keys [index-add index-del index-get reindex]} (indices-fn-ids name*)
         hs {[add-fn index-add] create [update-fn partial-fn reindex] update 
             [get-fn exists-fn all-fn get! exists! index-get] read [delete-fn index-del] delete }]
     (partition 2 (flatten (map (fn [[vs k]] (interleave vs (repeat k))) (filter (fn [[vs k]] (identity k)) hs))))))

(defmacro interceptors [name* opts]
 (let [{:keys [intercept]} (apply hash-map opts)]
    (concat (list 'do) (map (fn [[f c]] (list 'robert.hooke/add-hook (list 'var f) (list 'var c))) (hooks name* intercept))) 
   )) 

(macroexpand '(interceptors foo [:intercept {:create bar}] ))

(defmacro entity
  "Generates all the persistency (add/delete/exists etc..) functions for given entity"
  [f & r]
  (let [[meta* name* opts] (if (map? f) [f (first r) (rest r)] [{} f r])
        {:keys [id-fn delete-fn get-fn exists-fn all-fn]} (fn-ids name*)
        {:keys [index-del]} (indices-fn-ids name*)]
    `(do 
       (defn ~id-fn [~'id] (str '~name* ":" ~'id))

       (defn ~exists-fn [~'id] (not= 0 (wcar (car/exists (~id-fn ~'id)))))

       (index-fns ~name* ~opts)

       (defn ~get-fn 
        ([~'id] 
          (let [e# (wcar (car/hgetall* (~id-fn ~'id) true))]
            (with-meta (dissoc e# :meta) (e# :meta)) ))
        ([~'id & ks#] 
          (wcar (apply car/hget (~id-fn ~'id) ks#)))
         )

       (defn ~delete-fn [~'id] 
         (wcar 
           (~index-del ~'id (~get-fn ~'id)) 
           (car/del (~id-fn ~'id))))
 
       (defn ~all-fn []
         (map #(second (split % #":")) 
           (wcar
             (car/lua 
               "local newtbl= {}
                for i,v in pairs(redis.call('keys', _:keys)) do
                  if redis.call('type',v)['ok'] == 'hash' then
                    newtbl[i]=v
                  end
                end
                return newtbl"
                {} {:keys (str '~name* ":*")}))))

       (bang-fns ~name*)

       (write-fns ~name* ~opts ~meta*)

       (interceptors ~name* ~opts) 
       )))

