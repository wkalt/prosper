# Prosper service

A service for investment and activity monitoring on the Prosper.com P2P lending
marketplace. Investment functions are not suitable for your money. This is
under sporadic development and may never be completed. _Hooking it up to an
account with money or API investment enabled is a very bad idea._

## Current functionality

Queries Prosper on a user-defined interval and efficiently stores a snapshot of
the search/listings endpoint. The resulting data should be sufficient to
recreate the state of the market at a point in time with sub-second granularity.
Investment functionality is in an incomplete state and insufficiently tested.

### TODO
* historical data storage
* investment data storage
* investment reconciliation
* modeling plugs
* frontend

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

## Tests

Tests assume they will be run against the Prosper sandbox API. This will be a
separate set of credentials from your production API credentials, and
configuration is documented in profiles.example.clj. Do not run the tests
against a production account -- you could lose real money.
