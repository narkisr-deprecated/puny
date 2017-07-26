(ns puny.migrations
  "Support for migration that must happen outside interceptors like index add/delete etc.."
  (:require
   [subs.core :as subs :refer (validate!)]
   [puny.core :refer (entity)]))

(entity migration :id identifier :indices [operates-on])

(defn validate-migration [migration]
  (validate! migration
             {:applied #{:required :Boolean} :identifier #{:required :Keyword} :operates-on #{:required :Keyword}}
             :error ::non-valid-migration))

(defprotocol Migration
  "A general global migration (like adding indices)"
  (apply- [this] "Apply this migration")
  (rollback [this] "Rollback the miration (if possible)"))

(def migrations (atom {}))

(defn register
  "Registers a migration (order matters)"
  [operates-on {:keys [identifier] :as migration}]
  (swap! migrations update-in [operates-on] (fn [o] (conj (or o []) migration)))
  (when-not (migration-exists? identifier)
    (add-migration {:identifier identifier :operates-on operates-on :applied false})))

(defn applied?
  "checks if migration was already applied"
  [{:keys [identifier]}]
  (get-migration identifier :applied))

(defn flag-done
  "checks if migration was already applied"
  [{:keys [identifier]} target]
  (update-migration {:identifier identifier :applied true :operates-on target}))

(defn migrate
  "runs a list of migrations on provided target, skipping those that were already applied"
  [target]
  (doseq [m (target @migrations)]
    (try
      (when-not (applied? m)
        (.apply- m) (flag-done m target))
      (catch Throwable e (.rollback m) (throw e)))))

