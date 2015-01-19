(defproject couchdb-extension "0.1.2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.8"]
                 [org.clojure/data.json "0.2.4"]
                 [com.ashafa/clutch "0.4.0" :exclusions [clj-http]]]

  :plugins [[lein-midje "3.1.3"]
            [lein-pprint "1.1.1"]
            [lein-ancient "0.5.5"]]

  :repl-options {:welcome (println "Welcome to the magical world of the repl!")
                 :port 4001}
  ;  [lein-cloverage "1.0.3-SNAPSHOT"]
  :deploy-repositories [["releases" {:url "http://nexus.dsquare.intra/content/repositories/hps-releases"
                                     :sign-releases false}]
                        ["snapshots" {:url "http://nexus.dsquare.intra/content/repositories/hps-snapshots"
                                      :sign-releases false}]]
  :mirrors {"central" {:name "nexus"
                       :url "http://nexus.dsquare.intra/content/groups/public"}}

  :min-lein-version "2.0.0"

  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"] [midje "1.6.3"]
                                  [peridot "0.2.2"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha1"]]}}

  :aliases {"dev" ["do" "test"]})
