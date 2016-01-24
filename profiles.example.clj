{:dev {:env {:prosper {:username "prosper username"
                       :password "prosper password"
                       :client-id "clientid"
                       :client-secret "client secret"
                       :release-rate 333 ;; ms between requests. Rate limit is 20/sec.
                       :base-rate 6000
                       :storage-threads 4 ;; queue processing/response storage
                       :base-url "https://api.prosper.com/v1/"}
             :database {:subname "//localhost:5434/prosper"
                        :user "prosper"
                        :password "prosper"}
             :nrepl {:enabled true
                     :port 9090 }}}

 ;; DO NOT put your production API credentials in this section. Obtain sandbox
 ;; credentials to run the tests.
 :test {:env {:prosper {:username "sandbox username"
                        :password "sandbox password"
                        :client-id "sandbox client-id"
                        :client-secret "sandbox secret"
                        :release-rate 333
                        :base-rate 6000
                        :storage-threads 4
                        :base-url "https://api.sandbox.prosper.com/v1/"}
              :database {:subname "//localhost:5434/prosper"
                         :user "prosper"
                         :password "prosper"}
              :nrepl {:enabled true
                      :port 9090 }}}}
