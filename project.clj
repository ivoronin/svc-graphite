(defproject svc-graphite "0.1.0"
  :description "A program to send performance data of IBM SVC to Graphite"
  :url "https://github.com/ivoronin/svc-graphite"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.4"]
                 [clj-time "0.6.0"]
                 [clj-graphite "0.1.1"]]
  :main svc-graphite.core
  :profiles {:uberjar {:aot :all}})
