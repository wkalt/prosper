# Prosper service

This is a service for investment and activity monitoring on the Prosper.com
P2P lending marketplace. Currently its main function is to record a highly
granular history of the output of Prosper's 'search/listings' endpoint.
The investment side of the application is not complete or tested, and is not
suitable for production use.

## Requirements

* Postgres 9.5
* Prosper account with v1 API credentials (client-id and client-secret) and
  sandbox credentials if running tests.
* leiningen (to run from source)

## Running it

1. Create a Postgres user and database for the application:
    psql -c 'create role prosper with login'
    psql -c 'create database prosper owner prosper'

2. Copy profiles.example.clj to profiles.clj and resources/logback-example.xml
   to resources/logback.xml, and modify both to your needs.

3. Run the application with leiningen:
    lein prosper
