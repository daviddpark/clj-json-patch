(defproject clj-json-patch "0.1.7-SNAPSHOT"
  :description "Clojure implementation of http://tools.ietf.org/html/rfc6902"
  :url "http://github.com/daviddpark/clj-json-patch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main clj-json-patch.core
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [cheshire "5.10.0"]]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :profiles {:dev {:dependencies [[midje/midje "1.9.9"]
                                  [lambdaisland/kaocha "1.0.861"]
                                  [com.lambdaisland/kaocha-cljs "1.0.93"]]
                   :plugins [[lein-midje "3.2.2"]
                             [lein-doo "0.1.11"]]}}
  :aliases {"kaocha" ["run" "-m" "kaocha.runner"]}
  :doo {:paths {:karma "./node_modules/karma/bin/karma"}}
  :cljsbuild {:builds
              {:browser-test {:source-paths ["src" "test"]
                              :compiler {:output-to "target/cljsbuild/out/browser_tests.js"
                                         :main clj-json-patch.runner
                                         :optimizations :none}}}})
