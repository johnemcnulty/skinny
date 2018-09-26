package com.foo.elastic

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import org.json4s.JValue
import org.json4s.jackson.JsonMethods

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class ElasticSearchBaseServer(val host: String, val port: Int)(implicit val system: ActorSystem) {
  implicit lazy val ec = system.dispatcher // TODO: may not want to use system EC for read/write futures or even same EC for both reading and writing
  implicit lazy val materializer : Materializer = ActorMaterializer()
  protected implicit val format = org.json4s.DefaultFormats
}

trait ElasticSearchBaseWriteServer {
  self : ElasticSearchBaseServer =>

  protected def put(index: String, key: String, body: JValue)(implicit ec: ExecutionContext) : Future[JValue] = {
    val path = s"/${index.toLowerCase}/${index}/${key}"

    val bytes = ByteString(JsonMethods.compact(body))

    val request = HttpRequest(
      method = HttpMethods.PUT,
      uri = Uri(path),
      entity = HttpEntity(`application/json`, bytes)
    )

    val connectionFlow = Http().outgoingConnection(host, port)
    Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head).flatMap { response =>
      // TODO: ES returns a lot of detail about success, partial success, etc.  Interpret that here
      if (response.status == StatusCodes.OK || response.status == StatusCodes.Created) {
        import org.json4s.jackson.JsonMethods._
        Unmarshal(response).to[String].map(parse(_))
      } else {
        Future.failed(new Exception(s"status: ${response.status.intValue()}, message: " + response.entity.toString))
      }
    }
  }

  protected def del(index: String, key: String)(implicit ec: ExecutionContext) : Future[Try[Unit]] = {
    val path = s"/${index.toLowerCase}/${index}/${key}"

    val request = HttpRequest(
      method = HttpMethods.DELETE,
      uri = Uri(path)
    )

    val connectionFlow = Http().outgoingConnection(host, port)
    Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head).map { response =>
      // TODO: ES returns a lot of detail about success, partial success, etc.  Interpret that here
      if (response.status == StatusCodes.OK) {
        import org.json4s.jackson.JsonMethods._
        Success(())
      } else {
        Failure(new Exception(s"status: ${response.status.intValue()}, message: " + response.entity.toString))
      }
    }
  }
}

trait ElasticSearchBaseReadServer {
  self : ElasticSearchBaseServer =>

  protected def get(p: String): Future[String] = {

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
          Unmarshal(response).to[String]
        case StatusCodes.ServiceUnavailable =>
          Future.failed(new Exception(s"ElasticSearch not available at $host:$port"))
        case _ =>
          Future.failed(new Exception(s"status: ${response.status.intValue()}, message: " + response.entity.toString))
      }
    }
  }
}