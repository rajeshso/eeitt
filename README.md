# EEITT

[![Build Status](https://travis-ci.org/hmrc/eeitt.svg)](https://travis-ci.org/hmrc/eeitt) [ ![Download](https://api.bintray.com/packages/hmrc/releases/eeitt/images/download.svg) ](https://bintray.com/hmrc/releases/eeitt/_latestVersion)

This is the backend for the EEITT microservice.

## Pre-population API

There are two endpoints for persisting form pre-population data in production.

PUT        /eeitt/prepopulation/:cacheId/:formId

GET        /eeitt/prepopulation/:cacheId/:formId

In non-production environments, there is an extra endpoint, intended to facilitate testing

DELETE     /eeitt/prepopulation/:cacheId

See eeitt.raml for more formal detail

## Pre-population integration tests

When you run sbt it:test, it will fail with:

    [info]   java.net.ConnectException: Connection refused: localhost/127.0.0.1:8085

unless you first run smserver

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
