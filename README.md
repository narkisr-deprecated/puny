# Intro 

Puny is a tiny mapping layer from Clojure maps into redis hashes

[![Build Status](https://travis-ci.org/narkisr/puny.png?branch=master)](https://travis-ci.org/narkisr/puny)

# Usage

```clojure
  [puny 0.0.1]
```

Defining an without an id:

```clojure
(require '[puny.core :as p])

(p/entity foo)        

; validated-{entity} must be defined, it will be called upon add and update
(defn validate-foo [foo] {})

(let [id (add-foo {:bar 1})]
  (get-foo id) ;=> {:bar 1}
  (foo-exists? id) ;=> truthy
  (update-foo id {:bar 2}) 
  (get-foo id) ;=> {:bar 2}
  (delete-foo id)
  (foo-exists? id) ;=> falsey
)
```

An entity with a predefined id property:

```clojure
(p/entity {:ver 1} user :id name)        
(defn validate-user [user] {})
(add-user {:name "me"})]
(get-user "me") ;=> {:name "me"}
(user-exists? "me") ;=> truthy
(update-user {:name "me" :blue "bar"}) 
(get-user "me") ;=> {:name "me" :blue "bar"}
(delete-user "me")
(user-exists? "me") ;=> falsey
```

An entity with indexes:

```clojure
(p/entity car :id license :indices [color])        
(defn validate-car [car] {})
(add-car {:license 123 :color "black"}) ;=> 123
(add-car {:license 123 :color "black"}) ;=> :puny.integration/conflicting-car
(get-car 123) ;=> {:license 123 :color "black"}
(car-exists? 123) ;=> truthy
(get-car-index :color "black") ;=> ["123"]
(update-car {:license 123 :color "blue"}) 
(get-car-index :color "black") ;=> []
(get-car-index :color "blue") ;=> ["123"]
(delete-car 123)
(car-exists? 123) ;=> falsey
(get-car-index :color "blue") ;=> []
```

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
