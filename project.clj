(defproject sviepbd "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"] ; asynchrony
                 [http-kit "2.1.16"] ; High-performance, event-driven, asynchronous, Ring-compatible HTTP & Websocket client/server for clojure.
                 [org.jsoup/jsoup "1.7.3"] ; HTML DOM Manipulation
                 [ring/ring-codec "1.0.0"] ;; Utility library for encoding and decoding data
                 [cheshire "5.3.1"]; JSON encoding library for Clojure. 
                 [com.taoensso/timbre "3.1.6"]; Clojure logging and profiling library.
                 [org.clojure/java.jdbc "0.3.6"]; JDBC wrapper
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [clj-time "0.8.0"] ; A date and time library for Clojure, wrapping the Joda Time library.
                 [com.novemberain/monger "1.7.0"]; MongoDB driver.
                 [com.googlecode.htmlcompressor/htmlcompressor "1.4"][com.yahoo.platform.yui/yuicompressor "2.4.8"];; HTML minifier 
                 [org.clojure/data.csv "0.1.2"]
                 
                 ;; Stanford CoreNLP
                 [edu.stanford.nlp/stanford-corenlp "3.5.0"] ;; A Suite of Core Natural Language Processing Tools
                 [edu.stanford.nlp/stanford-corenlp "3.5.0" :classifier "models"]
                 
                 [net.mikera/core.matrix "0.31.1"] ;; Clojure matrix specification
                 [clatrix "0.4.0"] ;; native implementation of matrices using JBLAS
                 [org.la4j/la4j "0.4.9"] ;; Java LA library with sparse matrix support
                 
                 [org.xerial/sqlite-jdbc "3.7.2"]; SQLite java driver.
                 [com.h2database/h2 "1.3.170"] ;; H2
                 [korma "0.3.0"]; Korma : Clojure DSL for RDBMS.
                 ]
  :main ^:skip-aot sviepbd.core
  :repl-options {:init-ns sviepbd.google.crawler}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
