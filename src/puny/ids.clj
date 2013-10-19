(ns puny.ids
  "Misc id names of all functions, args etc.."
  (:require 
    [clojure.core.strint :refer (<<)] 
    [inflections.core :refer (plural)]))

(defmacro <<< 
  "String interpulation into a symbol"
  [s] `(symbol (<< ~s)))

(defmacro <<k 
  "String interpulation into a keyword"
  [s] `(keyword (<< ~s)))

(defn fn-ids [name*]
  {:id-fn (<<< "~{name*}-id") :exists-fn (<<< "~{name*}-exists?")
   :add-fn (<<< "add-~{name*}") :update-fn (<<< "update-~{name*}")
   :validate-fn (<<< "validate-~{name*}") :gen-fn (<<< "gen-~{name*}-id") 
   :delete-fn (<<< "delete-~{name*}") :get-fn (<<< "get-~{name*}")
   :partial-fn (<<< "partial-~{name*}") :merge-fn (<<< "merge-~{name*}")
   :all-fn (<<< "all-~(plural name*)")
   })

(defn id-modifiers [name* opts]
  "update arguments and key fn matching id-prop existence"
  (if-let [id-prop (opts :id)]
     {:up-args (vector {:keys [id-prop] :as 'v}) :up-id id-prop :add-k-fn (list 'v (keyword id-prop))}
     {:up-args ['id 'v] :up-id 'id :add-k-fn (list (:gen-fn (fn-ids name*)))}))
 
(defn indices-fn-ids [name*]
  {:index-add (<<< "index-~{name*}") :index-del (<<< "clear-~{name*}-indices")
   :index-get (<<< "get-~{name*}-index") :reindex (<<< "reindex-~{name*}") })

(defn ex 
   "Creates an exception key" 
   [name* type]
   (<<k "~{*ns*}/~{type}-~{name*}"))  

(defn bang-fn-ids [name*]
  (let [{:keys [id-fn delete-fn get-fn exists-fn ]} (fn-ids name*)] 
    {:missing-ex (ex name* "missing") 
     :exists! (<<< "~{name*}-exists!")  
     :delete! (<<< "delete-~{name*}!")
     :get! (<<< "get-~{name*}!")
     }))
 
