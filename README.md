# prosper

A clojure service for investing on Prosper.com

**HOW TO USE**

* install postgres 9.5

* copy /dev-resourcces/config.conf.example to /dev-resources/config.conf
and change your credentials

* Create a prosper account for yourself and obtain API credentials (this is free
but requires a bank account)

* Create an ini formatted config file like this:

[database]
subprotocol = postgresql
subname = //localhost:5432/prosper
user = prosper
password = prosper
classname = org.postgresql.Driver

[prosper]
username = <your prosper login>
password = <your prosper password>

* create the database and role specified in your config

psql -c 'create role prosper with login database'
psql -c 'create database prosper owner prosper'

* run it
lein tk

