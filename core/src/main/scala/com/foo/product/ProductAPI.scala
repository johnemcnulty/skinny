package com.foo.product

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.foo.{APIConfig, APIHelpers, Query}
import com.foo.Validation.ValidationError
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s
import org.json4s.JsonAST.JObject
import org.json4s.jackson.JsonMethods
import org.json4s.{Extraction, JValue}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class BadRequestException(issues: Map[String, String]) extends Throwable

case class ProductAPI(servicePath: String,
                      server: ProductServer,
                      validation: JValue => List[ValidationError],
                      config: APIConfig = APIConfig())
                     (implicit ec: ExecutionContext) {
  def route: Route = ProductAPI.routes(servicePath, server, validation)(ec, config)
}

object ProductAPI extends Json4sSupport with APIHelpers with LazyLogging {

  // Note: this is the magic incantation that makes routes happy with Future[JValue]'s
  implicit val serialization = json4s.jackson.Serialization // or native.Serialization

  import akka.http.scaladsl.server.Directives._

  def routes(servicePath: String, server: ProductServer, validation: JValue => List[ValidationError])
            (implicit ec: ExecutionContext, config: APIConfig) : Route = {

    val safePath = if (servicePath.startsWith("/")) servicePath.drop(1) else servicePath
    val routePath = separateOnSlashes(safePath)

    path(routePath / Segment / Segment) { (productType, id) =>
      get {
        val f = server.lookup(productType, id)
        onComplete(f) {
          case Success(response) =>
            response match {
              case RequestSucceeded(jv: JValue) => complete(StatusCodes.OK, renderSingletonResponse(servicePath, id, jv))
              case NotFound(_) => complete(StatusCodes.NotFound, renderNotFound(servicePath, productType, id))
              case BadRequest(errors) => complete(StatusCodes.BadRequest, renderBadRequest(servicePath, s"GET $id", errors))
              case InternalServerError(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
            }
          case Failure(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
        }
      } ~ delete {
        val f = server.delete(productType, id)
        onComplete(f) {
          case Success(response) => response match {
            case RequestSucceeded(_) => complete(StatusCodes.OK)
            case InternalServerError(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
          }
          case Failure(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
        }
      }
    } ~ path(routePath / Segment) { productType =>
      get {
        parameterMap { params =>
          Query.extractQuery(params) match {
            case Success(query) =>
              val f = server.query(productType, query.query, query.sortBy, query.sortDir, query.pageNum, query.pageSize)

              onComplete(f) {
                case Success(result) =>
                  result match {
                    case RequestSucceeded(results) => complete(StatusCodes.OK, renderResults(servicePath, results))
                    case BadRequest(errors) => complete(StatusCodes.BadRequest, renderBadRequest(servicePath, query.toString, errors))
                    case InternalServerError(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
                    case NotFound(_) => complete(StatusCodes.InternalServerError, renderError(servicePath, new Exception(s"Server returned not found for a query")))
                  }
                case Failure(t) => complete(StatusCodes.InternalServerError, t.getMessage)
              }
            case Failure(t : BadRequestException) =>
              logger.trace(s"Received bad request: $t")
              complete(StatusCodes.BadRequest, Extraction.decompose(t))
            case Failure(t) =>
              logger.trace(s"Received bad request: $t")
              complete(StatusCodes.BadRequest, t.toString)
          }
        }
      }
    } ~ path(routePath) {
      post {
        entity(as[JObject]) { jv =>
          validation(jv) match {
            case Nil =>
              val f = server.createOrUpdate(jv)
              onComplete(f) {
                case Success(fid) => fid match {
                  case RequestSucceeded(id) => complete(StatusCodes.OK, renderCreated(servicePath, id))
                  case BadRequest(errors) => complete(StatusCodes.BadRequest, renderBadRequest(servicePath, "createOrUpdate", errors))
                  case InternalServerError(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
                  case NotFound(_) => complete(StatusCodes.InternalServerError, renderError(servicePath, new Exception(s"Server returned not found for a query")))
                }
              }
            case errors => complete(StatusCodes.BadRequest, renderBadRequest(servicePath, "createOrUpdate", errors.map(RequestError(_))))
          }
        }
      }
    }
  }

}
