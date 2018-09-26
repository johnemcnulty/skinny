package com.foo.elastic.product

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.foo.elastic.{ElasticSearchBaseReadServer, ElasticSearchBaseServer}
import com.foo.location.Breadcrumb
import com.foo.product._
import com.foo.{ResourceId, ResourceType, product}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.JArray
import org.json4s.jackson.JsonMethods
import org.json4s.{JValue, JsonAST}

import scala.concurrent.{ExecutionContext, Future}

trait ElasticSearchReadServer extends ProductReadServer with ElasticSearchBaseReadServer with LazyLogging {
  self : ElasticSearchBaseServer =>

  override def lookup(resourceType: ResourceType, id: ResourceId): Future[Response[JsonAST.JValue]] = {
    val p = s"/${resourceType.toLowerCase}/${resourceType}/$id"

    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = Uri(p)
    )

    val connectionFlow = Http().outgoingConnection(host, port)
    Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head).flatMap { response =>
      response.status match {
        case StatusCodes.OK =>
          import org.json4s.jackson.JsonMethods._
          val fjv = Unmarshal(response).to[String].map(parse(_))
          fjv.map { jv => RequestSucceeded(jv \ "_source") }
        case StatusCodes.ServiceUnavailable =>
          Future.successful(InternalServerError(new Exception(s"ElasticSearch not available at $host:$port")))
        case _ =>
          Future.successful(InternalServerError(new Exception(s"status: ${response.status.intValue()}, message: " + response.entity.toString)))
      }
    }
  }

  override def query(productType: ResourceType, parameters: Map[String, String], sortBy: Option[String], sortDir: Option[String], pageNum: Int, pageSize: Int): Future[Response[QueryResults]] = {
    val query = parameters.map { case (k, v) => s"q=$k:$v" } mkString "&"
    val p = s"/$productType/$productType/_search?from=${pageNum*pageSize}&size=$pageSize&" + query
    get(p).map { str =>
      val jv = JsonMethods.parse(str)
      (jv \ "hits" \ "hits") match {
        case JArray(docs) =>
          val items = docs map { doc => (doc \ "_source") }
          val results = product.QueryResults(items.size, pageNum, pageSize, items)
          RequestSucceeded(results)
        case _ => InternalServerError(new Exception("Not found")) }
    }
  }

  protected def query(index: String, `type`: String, query: JValue, page: Int, pageSize: Int)
                     (implicit ec: ExecutionContext) : Future[JValue] = {
    val p = s"/${index}/${`type`}/_search?from=${pageSize*page}&size=$pageSize"

    val bytes = ByteString(JsonMethods.compact(query))

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = Uri(p),
      entity = HttpEntity(`application/json`, bytes)
    )

    val connectionFlow = Http().outgoingConnection(host, port)
    Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head).flatMap { response =>
      if (response.status == StatusCodes.OK) {
        import org.json4s.jackson.JsonMethods._
        Unmarshal(response).to[String].map(parse(_))
      } else {
        Future.failed(new Exception(s"status: ${response.status.intValue()}, message: " + response.entity.toString))
      }
    }
  }
}
