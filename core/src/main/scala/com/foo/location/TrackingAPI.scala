package com.foo.location

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.foo.Validation.ValidationError
import com.foo._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.JValue

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object TrackingAPI extends Json4sSupport with APIHelpers with LazyLogging {

  // Note: this is the magic incantation that makes routes happy with Future[JValue]'s
  implicit val serialization = org.json4s.jackson.Serialization // or native.Serialization

  import akka.http.scaladsl.server.Directives._

  def routes(servicePath: String, server: LocationServer)
            (implicit ec: ExecutionContext, config: APIConfig) : Route = {
    val safePath = if (servicePath.startsWith("/")) servicePath.drop(1) else servicePath
    val routePath = separateOnSlashes(safePath)

    path(routePath / Segment / Segment) { (productType, productId) =>
      get {
        parameters('lat.as[Float], 'lon.as[Float], 'time) { (lat, lon, time) =>
          val f = server.track(Breadcrumb(productType, productId, Location(lat, lon, time)))
          onComplete(f) {
            case Success(_) => complete(StatusCodes.OK)
            case Failure(t) => complete(StatusCodes.InternalServerError, renderError(servicePath, t))
          }
        }
      }
    } ~ path(routePath / Segment / Segment / "latest") { (productType, productId) =>
      get {
        val f = server.location(productType, productId)
        onComplete(f) {
          case Success(result) =>
            result match {
              case Success(loc) => complete(StatusCodes.OK, render(loc))
              case Failure(t) => complete(StatusCodes.InternalServerError, renderError(s"$servicePath/location/$productType/$productId", t))
            }
          case Failure(t) => complete(StatusCodes.InternalServerError, t.getMessage)
        }
      }
    } ~ path(routePath / Segment / Segment / "locations") { (productType, productId) =>
    get {
      val f = server.history(productType, productId)
      onComplete(f) {
        case Success(result) =>
          result match {
            case Success(loc) => complete(StatusCodes.OK, renderLocationHistory(loc))
            case Failure(t) => complete(StatusCodes.InternalServerError, renderError(s"$servicePath/locations/$productType/$productId", t))
          }
        case Failure(t) => complete(StatusCodes.InternalServerError, t.getMessage)
      }
    }
  }
  }

}
