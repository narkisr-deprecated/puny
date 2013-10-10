# Intro 

Puny is a tiny layer for persisting Clojure maps into Redis hashes, it aims to reduce boilerplate and to enable end to end life cycle management of entities from validation to data migration and indexing.


It includes support for:

* Complete generated CRUD API.
* Automatic keys and id management (both generated and internal to the entity).
* Automatic Validation checks.
* Declaring indexes.
* Attaching persisted metadata like versioning etc..
* Defining interceptors on CRUD functions enabling support for security checks, automatic migration etc..

Puny is based on [carmine](https://github.com/ptaoussanis/carmine), a big thank you to Peter its author.

[![Build Status](https://travis-ci.org/narkisr/puny.png?branch=master)](https://travis-ci.org/narkisr/puny)

# Usage

```clojure
 [puny "0.2.1"]
```

## Walk through

Defining an entity with a generated id:

```clojure
(require '[puny.core :as p])

(p/entity foo)        

; validated-{entity} must be defined, it will be called upon add and update
(defn validate-foo [foo] {})

```

Basic CRUD:
```clojure 
(let [id (add-foo {:bar 1})]
  (get-foo id) ;=> {:bar 1}
  (foo-exists? id) ;=> truthy
  (update-foo id {:bar 2}) 
  (get-foo id) ;=> {:bar 2}
  (delete-foo id)
  (foo-exists? id) ;=> falsey
)
```

An entity with a internal id property:

```clojure
(p/entity user :id name)        
(defn validate-user [user] {})
(add-user {:name "me"})]
(get-user "me") ;=> {:name "me"}
```

Fail fast functions:

```clojure
(p/entity planet :id name :indices [life])
(defn validate-planet [planet] {})
(add-planet {:name "lunar" :life "false"})
(planet-exists! "lunar"); => true
(planet-exists! "foo"); => (throws ExceptionInfo (is-type? :puny.test.core/missing-planet))
(delete-planet! "foo"); => (throws ExceptionInfo (is-type? :puny.test.core/missing-planet))

```

Fast retrieval of all entity keys:

```clojure
 (p/entity phone :id number :indices [type])
 (defn validate-phone [p] {})
 (add-phone {:number 1234 :type "android"})
 (add-phone {:number 1235 :type "iOS"}) 
 (all-phones); => '("1234" "1235")
    
```

Defining indexes:

```clojure
(p/entity car :id license :indices [color])        
(defn validate-car [car] {})
(add-car {:license 123 :color "black"}) ;=> 123
(get-car-index :color "black") ;=> ["123"]
```

Metadata and hooks (automatic versioning):

```clojure
(def current-version 2)

(declare update-car)

(defn upgrade-car [f & args] 
    (let [res (apply f args) version (-> res meta :version)]
      (if (and (map? res) (or (nil? version) (> current-version version )))
        (do
          (update-car (assoc res :tires "big")) (apply f args))
        res) 
      ))

; each entity that is read gets update automaticly, versioning info is set in metadata
(p/entity {:version current-version} car :id license 
    :indices [color] :intercept {:read [upgrade-car]})

(defn validate-car [car] {})

(add-car {:license 123 :color "black"}) => 123

; we set the version back to 1 to trigger update
(wcar (car/hset (car-id 123) :meta {:version 1}))

; the entity gets updated on the fly
(:tires (get-car 123)) ;=> {:license 123 :color "black" :tires "big"}
```

## Caveats
Puny is not transactional, some underlying operations might span multiple operations, in case where consistency and atomicity are important its recommended to use [locks](https://github.com/ptaoussanis/carmine/blob/master/src/taoensso/carmine/locks.clj) on entity ids.

# Copyright and license

Copyright [2013] [Ronen Narkis]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
