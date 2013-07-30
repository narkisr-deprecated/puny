(defproject puny "0.1.1"
  :description "A puny map to redis persistency layer"
  :url "https://github.com/narkisr/puny"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/carmine "2.0.0"]
                 [org.flatland/useful "0.10.3"]
                 [org.clojure/core.incubator "0.1.3"]
                 [inflections "0.8.1"]]
  
  :plugins  [[jonase/eastwood "0.0.2"] [lein-midje "3.0.0"] [lein-ancient "0.4.2"]
             [lein-tag "0.1.0"] [lein-set-version "0.3.0"]]

  :aliases {"autotest"
            ["midje" ":autotest" ":filter" "-integration"] 
            "runtest"
            ["midje" ":filter" "-integration"] 
            }
  :profiles {:dev {
               :source-paths  ["dev"]
               :dependencies [[org.clojure/tools.trace "0.7.5"] [midje "1.5.1"] [junit/junit "4.11"]]
              
              :set-version {
                  :updates [ 
                    {:path "README.md" :search-regex #"\"\d+\.\d+\.\d+\""}]}
               }
             }

  )
