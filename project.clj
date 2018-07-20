(defproject clj-json-patch "0.1.5"
  :description "Clojure implementation of http://tools.ietf.org/html/rfc6902"
  :url "http://github.com/daviddpark/clj-json-patch"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main clj-json-patch.core
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.3.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.0.0"]]}})
