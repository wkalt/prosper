{:dev {:env {:prosper {:username "my prosper username"
                       :password "my prosper password"
                       :client-id "my client id"
                       :client-secret "my client secret"
                       :release-rate 333
                       :base-rate 6000
                       :storage-threads 4
                       :weekday-release ["9:00" "17:00"]
                       :weekend-release ["12:00"]}
             :database {:subprotocol "postgresql"
                        :subname "//localhost:5434/prosper"
                        :user "prosper"
                        :password "prosper"
                        :classname "org.postgresql.Driver"}
             :global {:logging-config "./dev-resources/logback-dev.xml"}
             :nrepl {:enabled false
                     :type "nrepl"
                     :port 9090 }}}

 :test {:env {:prosper {:username "my prosper username"
                        :password "my prosper password"
                        :client-id "my client id"
                        :client-secret "my client secret"
                        :release-rate 333
                        :base-rate 6000
                        :storage-threads 4
                        :weekday-release ["9:00" "17:00"]
                        :weekend-release ["12:00"]}
              :database {:subprotocol "postgresql"
                         :subname "//localhost:5434/prosper"
                         :user "prosper"
                         :password "prosper"
                         :classname "org.postgresql.Driver"}
              :global {:logging-config "./dev-resources/logback-dev.xml"}
              :nrepl {:enabled false
                      :type "nrepl"
                      :port 9090 }}}}
