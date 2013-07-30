# Intro 

Puny is a tiny mapping layer from Clojure maps into redis hashes

# Usage

```clojure
  [puny 0.0.1]
```

Defining an entity:

```clojure
(require '[puny.core :as p])

(p/entity foo)        

; a validation fn named validated-{entity} must be defined, it will be called upon add and update
(defn validate-foo [foo] {})

(let [id (add-foo {:bar 1})]
  (get-foo id) => {:bar 1}
  (foo-exists? id) => truthy
  (update-foo id {:bar 2}) 
  (get-foo id) => {:bar 2}
  (delete-foo id)
  (foo-exists? id) => falsey)
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
