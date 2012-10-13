(defproject clj-mixpanel "0.0.1-SNAPSHOT"
  :description "clojure api for the mixpanel"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.json "0.1.3"]
                 [clj-http "0.5.3"]
                 [clj-time "0.4.4"]]
  :plugins [[lein-release "1.0.0"]
            [lein-midje "2.0.0-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}

  :repositories {"snapshots"
                 {:url "http://ci.copious.com:8082/nexus/content/repositories/snapshots/"
                  :username "deployment" :password "Q5Erm4JqFppGSf"}
                 "releases"
                 {:url "http://ci.copious.com:8082/nexus/content/repositories/releases/"
                  :username "deployment" :password "Q5Erm4JqFppGSf"}})
