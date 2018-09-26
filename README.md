# Getting started

* Download `elastic search 5.2.0` here https://www.elastic.co/downloads/past-releases/elasticsearch-5-2-0
* unpack or unzip in to some directory `/path/to/es`
* `cd /path/to/es`
* `bin/elasticsearch`

To run the prototype
* `cd /path/to/prototype`
* `sbt "project core" run`

You should see `API binding 9000` on `stdout` when the service is up and running

# APIs

Two APIs defined:
* product API for product CRUD
* tracking API for location tracking data for products

All API endpoints are RESTful in the following sense:
* `200 OK` on a successful request
* `400 Bad Request` for a poorly formed request
* `500 Internal Server Error` if the request was OK but the server blew it

## Product API

* `GET /product/<productType>/<id>` get a product instance by type and id
* `POST /product/<productType>` create a product instance of the specified type
* `GET /product/<productType>?<parameters>` query for products of the specified type (details below)
* `DELETE /product/<productType>/<id>` delete product instance by type and id

Query parameters take the following form:

  `name=value`
  
where `name` is a "dotted name" path into a JSON object.  For example, given the
following JSON object 

```
{
   attributes {
      a : 1
      inner {
         b : 2 
      }
   }
   location {
      lat : 0.0
      lon : 0.0
   }
}
```

`attributes.inner.b` and `location.lat` are both "dotted names" identifying specific
fields within the JSON object/schema.

If the JSON object above were a `coffee` product then we could find it with the following
query:

`/product/coffee?attributes.inner.b=2`

## Tracking API 

* `GET /track/<productType>/<productId>?lat=<lat>&lon=<lon>&time=<time>`

where `<lat>` and `<lon>` are floats and `<time>` has the form `2018-09-24T14:12:00Z`

* `GET /track/<productType>/<productId>/latest` last known location of specified item
* `GET /track/<productType>/<productId>/locations` all known locations sorted by time

In prototype implementation it is possible to association location information with a
non-existent product instance.

## Product Schemas

Products are defined by adding `JsonSchema` files to the directory `/schemas/products` with 
file names like `coffee.schema.json`:
* `coffee` will be the product type
* A new Product API endpoint is created at `/product/coffee`
* A new `ElasticSearch` index (i.e. table) will be created at `/coffee/coffee` (the first
`coffee` is the index and the second `coffee` is the document type.  Best practice is
one document type per index, believe this is enforced in ES 6+)

New product schemas are available after restart. 