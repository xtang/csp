(defproject csp "0.1.0-SNAPSHOT"
  :description "play with core.async"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [http-kit "2.1.18"]
                 [rome/rome "1.0"]]
  :main ^:skip-aot csp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
