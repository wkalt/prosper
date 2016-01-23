(def ks-version "1.2.0")

(defproject prosper/prosper "0.2.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0-RC2"]
                 [environ "1.0.1"]
                 [silasdavis/at-at "1.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [clj-time "0.9.0"]
                 [http-kit "2.1.16"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.3.1"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.nrepl "0.2.3"]]

  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]

  :aliases {"prosper" ["trampoline" "run"]}

  :main prosper.service)
