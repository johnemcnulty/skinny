package com.foo.elastic.location

import java.net.URLEncoder

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import com.foo.elastic.{ElasticSearchBaseReadServer, ElasticSearchBaseServer}
import com.foo.location.{Breadcrumb, LocationReadServer, Path}
import com.foo.{ResourceId, ResourceType}
import com.typesafe.scalalogging.LazyLogging
import org.json4s._
import org.json4s.jackson.{JsonMethods, Serialization}
import org.json4s.jackson.Serialization.read
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ElasticSearchLocationReadServer extends LocationReadServer with ElasticSearchBaseReadServer with LazyLogging {
  self : ElasticSearchBaseServer =>

  implicit val formats = Serialization.formats(NoTypeHints)

  override def history(productType: String, productId: String): Future[Try[Path]] = {
    val query = s"q=productType:$productType&q=productId:$productId&sort=location.time:asc"
    val p = s"/locations/_search?" + query
    get(p).map { str =>
      val jv = JsonMethods.parse(str)
      (jv \ "hits" \ "hits") match {
        case JArray(docs) =>
          val breadcrumbs = docs map { doc => (doc \ "_source").extract[Breadcrumb].location }
          Success(Path(productType, productId, breadcrumbs))
        case _ => Failure(new Exception("Not found")) }
    }
  }

  override def location(productType: ResourceType, productId: ResourceId): Future[Try[Breadcrumb]] = {
    val key = URLEncoder.encode(s"${productType}.${productId}", "UTF-8")
    val p = s"/latest/$key"
    get(p).map { str => Success(read[Breadcrumb](str)) }
  }
}
