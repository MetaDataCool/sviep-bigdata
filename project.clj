(defproject sviepbd "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"] ; asynchrony
                 [http-kit "2.1.16"] ; High-performance, event-driven, asynchronous, Ring-compatible HTTP & Websocket client/server for clojure.
                 [org.jsoup/jsoup "1.7.3"] ; HTML DOM Manipulation
                 [com.taoensso/timbre "3.1.6"]; Clojure logging and profiling library.
                 ]
  :main ^:skip-aot sviepbd.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
