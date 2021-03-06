(defproject prosper/prosper "0.2.1-SNAPSHOT"
  :description "Prosper service"
  :dependencies [[org.clojure/clojure "1.8.0-RC2"]
                 [com.cemerick/url "0.1.1"]
                 [environ "1.0.1"]
                 [throttler "1.0.0"]
                 [silasdavis/at-at "1.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [clj-time "0.9.0"]
                 [http-kit "2.1.16"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.3.1"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [ch.qos.logback/logback-classic "1.1.3"]]

  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]

  :aliases {"prosper" ["trampoline" "run"]}

  :main prosper.service)
