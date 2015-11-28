# prosper

A clojure service for investing on Prosper.com

**HOW TO USE**

* install postgres 9.5

* Create a prosper account for yourself and obtain API credentials (this is free
but requires a bank account)

* copy /dev-resourcces/config.conf.example to /dev-resources/config.conf
and modify according to your uses

* copy profiles.example.clj to profiles.clj and add your prosper credentials

* create the database and role specified in your config

psql -c 'create role prosper with login database'
psql -c 'create database prosper owner prosper'

* run it
lein prosper -c dev-resources/config.conf
