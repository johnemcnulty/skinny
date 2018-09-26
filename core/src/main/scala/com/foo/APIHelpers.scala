package com.foo

import com.foo.location.{Breadcrumb, Location, Path}
import com.foo.product.{QueryResults, RequestError}
import org.json4s.JsonAST._
import org.json4s.{DefaultFormats, JValue}

import scala.concurrent.ExecutionContext

/*
 * These methods are in APIHelpers to avoid conflicting definitions of implicit ~ when co-resident with akka routes
 */
trait APIHelpers {

  import org.json4s.JsonDSL._
  implicit val formats = DefaultFormats

  def renderCreated(servicePath: String, id: ResourceId) : JValue = {
    ("data" -> ("id" -> id))
  }

  def renderResults(servicePath: String, qr: QueryResults) : JValue = {
    ("data" -> qr.data.toList)
  }

  def render(loc: Breadcrumb) : JValue = {
    ("data" -> ("productType" -> loc.productType) ~ ("productId" -> loc.productId) ~ ("lat" -> loc.location.lat) ~ ("lon" -> loc.location.lon) ~ ("time" -> loc.location.time))
  }

  def renderLocationHistory(loc: Path) : JValue = {
    val path : List[JValue] = loc.locations.map { loc => ("lat" -> loc.lat) ~ ("lon" -> loc.lon) ~ ("time" -> loc.time) } toList;
    ("data" -> ("productType" -> loc.productType) ~ ("productId" -> loc.productId) ~
      ("history" -> path))
  }

  def renderSingletonResponse(servicePath: String, id: String, r: JValue) : JValue = {
    ("data" -> r)
  }

  def renderNotFound(servicePath: String, productType: ResourceType, id: String) : JValue = {
    ("errors" -> ("detail" -> s"Not found: $productType:$id"))
  }
  def renderError(servicePath: String, t: Throwable) : JValue = {
    ("errors" -> ("detail" -> t.getMessage))
  }

  def renderBadRequest(servicePath: String, desc: String, errors: List[RequestError]) : JValue = {
    ("errors" -> (errors map { _.desc }))
  }
}
