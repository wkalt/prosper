(def ks-version "1.0.0")
(def tk-version "1.0.1")
(def tk-jetty9-version "1.0.0")

(defproject prosper/prosper "0.2.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [environ "1.0.1"]
                 [overtone/at-at "1.2.0"]
                 [compojure "1.2.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [clj-time "0.9.0"]
                 [http-kit "2.1.16"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [cheshire "5.3.1"]
                 [clj-http "0.9.2"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [puppetlabs/trapperkeeper ~tk-version]
                 [puppetlabs/trapperkeeper-webserver-jetty9 ~tk-jetty9-version]]

  :profiles {:dev {:dependencies [[puppetlabs/trapperkeeper ~tk-version :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink ~ks-version :classifier "test" :scope "test"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}

  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]

  :aliases {"prosper" ["trampoline" "run" "--config" "dev-resources/config.conf"]}
  :aot [puppetlabs.trapperkeeper.main]

  :main puppetlabs.trapperkeeper.main)
