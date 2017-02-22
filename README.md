# EEITT

[![Build Status](https://travis-ci.org/hmrc/eeitt.svg)](https://travis-ci.org/hmrc/eeitt) [ ![Download](https://api.bintray.com/packages/hmrc/releases/eeitt/images/download.svg) ](https://bintray.com/hmrc/releases/eeitt/_latestVersion)

This is the backend for the EEITT microservice.

## Pre-population API

There are two endpoints for persisting form pre-population data in production.

PUT        /prepopulation/:cacheId/:formId

GET        /prepopulation/:cacheId/:formId

In non-production environments, there is an extra endpoint, intended to facilitate testing

DELETE     /prepopulation/:cacheId

The cacheId parameter, a string, is used to identify the owner of the data, likely to by the Government Gateway GID or CID, depending on the functionality required.
  
The formId parameter, also a string, identifies a particular data-set, it could be the identifier of a form page, but where multiple forms share common persisted data, then one way or another a common identifier for that data needs to be used.

In the request or response body, the data itself is just Media Type application/json, and can be arbitrary JSON as far as the eeitt service is concerned.

Data may not be persisted after 28 days.

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
