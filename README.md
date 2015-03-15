# prosper

A clojure service for investing on Prosper.com

**TODO**:

- web interface
- benchmark
- testing
- simulated service / replay
- selection model
- ec2 (do) interop
- schema validation

**HOW TO USE**

create an ini formatted config file like this:

[database]
subprotocol = postgresql
subname = //localhost:5432/prosper
user = prosper
password = prosper
classname = org.postgresql.Driver

[prosper]
username = <your prosper login>
password = <your prosper password>

create the database and role specified in your config

psql -c 'create role prosper with login database'
psql -c 'create database prosper owner prosper'

migrate to the latest version:

lein run migrate -c config.ini


run collection to test:

lein run collection -c config.ini -d 10
