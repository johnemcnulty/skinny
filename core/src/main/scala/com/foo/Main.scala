package com.foo

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.foo.elastic.product.ElasticSearchProductServer
import com.foo.elastic.ElasticSearchBaseServer
import com.foo.elastic.location.ElasticSearchLocationServer
import com.foo.location.TrackingAPI
import com.foo.product.ProductAPI
import com.typesafe.scalalogging.LazyLogging
import org.json4s
import org.json4s.DefaultFormats

// Copyright (C) 2006-2018 by Ciena, Inc
//
// All rights reserved.
//
// PROPRIETARY NOTICE
//
// This Software consists of confidential information.
// Trade secret law and copyright law protect this Software.
// The above notice of copyright on this Software does not indicate
// any actual or intended publication of such Software.


object Main extends LazyLogging {
  def main(args: Array[String]): Unit = {

    implicit val system = ActorSystem("foo")

    implicit val materializer : Materializer = ActorMaterializer()

    import akka.http.scaladsl.server.Directives._

    import scala.concurrent.duration._

    val http = Http(system)
    implicit val serialization = json4s.jackson.Serialization // or native.Serialization
    implicit val formats = DefaultFormats
    implicit val timeout = Timeout(5 seconds)

    implicit val config = APIConfig(system.settings.config.getConfig("api"))
    val productServer = new ElasticSearchBaseServer("localhost", 9200) with ElasticSearchProductServer
    val locationServer = new ElasticSearchBaseServer("localhost", 9200) with ElasticSearchLocationServer

    val validation = new Validation("schemas/products")

    val hardcoded = path("ping") {
      get { complete(HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "PONG!"))) }
    } ~ ProductAPI.routes("/product", productServer, validation.validate _)(system.dispatcher, config) ~
    TrackingAPI.routes("/track", locationServer)(system.dispatcher, config)

    logger.info(s"API binding 9000")
    Http().bindAndHandle(hardcoded, "localhost", 9000)

  }
}
