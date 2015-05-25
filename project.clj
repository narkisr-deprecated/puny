(defproject puny "0.3.0"
  :description "A puny map to redis persistency layer"
  :url "https://github.com/narkisr/puny"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [robert/hooke "1.3.0"] 
                 [com.taoensso/carmine "2.10.0"]
                 [substantiation "0.2.1"]
                 [org.flatland/useful "0.11.3"]
                 [org.clojure/core.incubator "0.1.3"]
                 [inflections "0.9.14"]]
  
  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.1.3"] [lein-ancient "0.6.7"]
             [lein-tag "0.1.0"] [lein-set-version "0.3.0"]]

  :aliases {
            "autotest" ["midje" ":autotest" ":filter" "-integration"] 
            "runtest" ["midje" ":filter" "-integration"]
           }

  :profiles {
    :dev {
        :source-paths ["dev"]
        :dependencies [[midje "1.6.3"] [org.clojure/tools.trace "0.7.8"]]
              
        :set-version {
            :updates [ {:path "README.md" :search-regex #"\"\d+\.\d+\.\d+\""}]
        }
    }
  }

)
