# RADL

[![license](https://img.shields.io/github/license/restful-api-description-language/RADL.svg)](https://raw.githubusercontent.com/restful-api-description-language/RADL/master/LICENSE)
[![Version](https://img.shields.io/github/release/restful-api-description-language/radl.svg)](https://github.com/restful-api-description-language/RADL/releases)
![Build Status](https://travis-ci.org/restful-api-description-language/RADL.svg?branch=master) [![codecov.io](https://img.shields.io/codecov/c/github/restful-api-description-language/RADL.svg)](https://codecov.io/github/restful-api-description-language/RADL)

* [What is RADL?](#What_is_RADL)
* [How can I get RADL?](#How_can_I_get_RADL)
* [How do I use RADL?](#How_do_I_use_RADL)
* [Getting help](#Getting help)
* [License](#License)

![logo](logo.png "Logo")


## <a name="What_is_RADL"/> What is RADL? ##

*RESTful API Description Language* (RADL) is an XML vocabulary for
describing Hypermedia-driven RESTful APIs.  Unlike most HTTP API description languages, RADL focuses on defining a truly hypermedia-driven REST API from the client's point of view. Unlike description languages based on JSON or Markdown, RADL makes it easy to integrate documentation written in HTML or XML. The APIs that RADL describes may use any media type, in XML, JSON, HTML, or any other format.

RADL can be used to:

* Design a RESTful API
* Validate an API description for consistency
* Generate documentation from an API description
* Generate Spring Framework Java controller classes from an API description (for design-first environments)
* Generate the resource model for a RESTful API from Spring Framework of JAX-RS Java controller classes (for code-first environments)

> *NOTE* - RADL does not yet support non-Java environments like Node.js, and we have not yet defined a timeline for doing so. If you need support for a particular environment in RADL, please contact the authors - we welcome contributed code to support such environments.

RADL describes both the hypermedia client API and the resource model, which describes the implementation and should not be exposed to the client.
The client API is defined in terms of client states, link relations, media types, URI parameters, and any custom headers or status codes that need to be documented.

The resource model is defined in terms of resources, their locations, the HTTP methods that can be applied to them, and the conventions for using those methods.  The resource model corresponds roughly to the information found in Swagger or RAML.

In documentation generated by RADL, the interfaces associated with each client state are assembled from the client API description and the resource model.

RADL is formally defined in the [RADL Specification](specification/spec.md).


##  <a name="How_can_I_get_RADL"/> How can I get RADL? ##

* Get the [latest binary release](https://github.com/restful-api-description-language/RADL/releases)
* Get from BinTray's Maven repository using the repository URL `http://dl.bintray.com/radl/RADL`
* Get the source code using `git clone https://github.com/restful-api-description-language/RADL`


##  <a name="How_do_I_use_RADL"/> How do I use RADL?

See the [documentation](https://github.com/restful-api-description-language/RADL/wiki).


## Getting help ##

RADL is an open source project and is under active development. If you need help, would like to contribute, or simply 
want to talk about the project with like-minded individuals, we have a number of open channels for communication:

* To report bugs or file feature requests: please use the [issue tracker on Github](https://github.com/restful-api-description-language/RADL/issues).
* To talk about the project please post a message to [RADL HipChat room](https://radl.hipchat.com/chat). This is also
where you can find out what's going on with the project.
* To contribute code or documentation changes: please submit a [pull request on Github](https://github.com/restful-api-description-language/RADL/pulls).


## <a name="License"/> License ##

Copyright � EMC Corporation. All rights reserved. EMC grants rights under the Apache 2.0 license.
See the [`LICENSE`](https://raw.githubusercontent.com/restful-api-description-language/RADL/master/LICENSE) file for more information.
