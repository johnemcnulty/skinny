# Assumptions and simplifications

The focus of this prototype was to demonstrate how we can define strict
product schemas while retaining open-ended extensibility in both
* adding attributes to existing products
* adding new products 

# Architecture

High-level:  
* `ElasticSearch` backend supporting APIs implemented using Akka HTTP.  
* Product types defined as `JsonSchema` files in a configuration directory

Internal:
* API is `akka route` backed by a "server"
* API supports all product types found in the `/schemas/products` directory on startup
* API validates all incoming requests against the appropriate `JsonSchema` and provides detailed error messages
* server definition is split into read server and write server traits to permit separating them down the road for scalablity
* server implementations provided for `ElasticSearch` 

`JsonSchema` enables us to construct product schemas with common attributes and
attributes unique to a subset or even a single product type.

`ElasticSearch` enables us to forego `index` (aka `table`) definitions since `ElasticSearch`
will create an `index` on the fly when you first write a document to that `index` and
attempt to work out the schema by inspecting the incoming documents.

The downside to this default behavior is that `ElasticSearch` indexes _everything_ and
can become quite a resource hog with these bloated indexes.

# API Design

Decision was taken to create new API paths/endpoints for each product type like `/product/coffee` on
the assumption that some of these product categories will operate as independent verticals.  There is
no reason that the APIs couldn't be combined into a single `/product` API or into groups of
products in a vertical API like "seafood" with multiple products.  The distinct `ElasticSearch` indexes
would be maintained however so `ElasticSearch` can index different product types efficiently.  Having said that,
`ElasticSearch` would be fine with combining product types with largely identical schemas with a few special attributes
here and there as long as the special attributes do not overlap and require contradictory definitions.

# Future considerations

### framework

Seems like a tailormade exercise for a CRUD framework like Grails but I haven't used Grails, play, etc for a
number of years and for a quick prototype like this I like to follow a "9+1 rule": 9 familiar tools + 1 exploratory
In this case the exploratory technology was JsonSchema so I was building the CRUD stack by hand.
 
### access requirements

The default behavior of `ElasticSearch` is very generous but also very generous with our RAM.  
One solution is explicit `access requirements` (considered but not pursued in this prototype).

An `access requirement` specifies for a given product type exactly which attributes need to be queryable 
and precisely how.  For example, do we need to search on the `vesselId` field of a `fish` item?  
If so, is that exact string search? case insensitive?  substring?  type-ahead prefix?  type-ahead any 
part of the field?  All of these considerations influence (a) how many different ways we would index a given 
field if at all and (b) which queries the API permits to pass thru to the server.   For example, if we don't 
have a search requirement for `vesselId` and the API were to permit a query against an unindexed field, this 
could seriously bog down the server.

### unit tests

Unit test coverage is more illustrative than comprehensive.

### admin APIs

need to create owners, give them access, set up terms of service/payments, etc.  something for the support 
team to use as well -- being able to authenticate as a customer and see what they see, for example.

### API documentation

Should have at least swagger docs for the APIs but given the extensible product schemas this
would have entailed implementing a translation of JsonSchema documents into Swagger spec
bits and pieces.  Not sure if tools exist for doing this already but didn't want to attempt
an investigation or implementation in the available time.

### bulk APIs

Should probably have APIs for both bulk uploading and downloading of product data.  It's easy to forget that
bulk APIs are different beasts and just pointing clients to your usual search APIs is a recipe for some late
nights.

### batch writes

`ElasticSearch` write performance isn't going to win any awards so implementing batch writes is advisable
for any reasonable level of traffic.

### dependency injection

Just wired things up ad-hoc for the prototype but DI is essential for maintaining cleanly defined components
and in particular run-time DI if possible.

run-time dependency injection enables a functional component to express a dependency on another component by:
* name
* role
* facet (the other components share some characteristic or express some "facet")

and have that dependency by resolved at run-time.  This capability greatly facilitates moving
 components/features around a cluster in the future as things scale up.

### services and service discovery

micro-services and of course service discovery would be useful.  Here we just assumed service locations were
available in config and of course the service itself isn't package for deployment.  some framework for deploying and managing a coherent collection of micro-services would
become essential after the first handful were created

### libraries

can't build micro-services without good libraries.  This project was setup as multi-project to facilitate separate
libraries but didn't use that option.  For example, the `ElasticSearch` base classes would make a nice library to 
be shared across micro-services.

### permissions

ignored authentication, authorization, permissions altogether.  Very easy to imagine product owners wanting to 
selectively share their offerings with other parties.  Modeling those sharing/permissions arrangements would take
some thought as well as implementing them so that queries were still performant even when executed thru a 
permissions filter.

### localization

pretty important for this domain

### clustering, replication, failover

not just clustering `ElasticSearch` for example but also designing clients that can deal with a cluster and
that can work with service discovery in case _all_ known members of a service cluster disappear (i.e., gossip
protocols can only get you so far)

### fixtures

would have been nice to generate a bunch of test data in `ElasticSearch`, test those implementations and then
tear it all down

### logging, monitoring, metrics, diagnostic APIs

monitoring for issues, performance etc.  log aggregation for diagnostics across micro-services.  diagnostic
APIs in some services to provide insight into its internal state/consistency in case monitoring flags something




