package com.foo.elastic.location

import java.net.URLEncoder

import com.foo.ResourceId
import com.foo.elastic.{ElasticSearchBaseServer, ElasticSearchBaseWriteServer}
import com.foo.location.{Breadcrumb, LocationWriteServer}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.Extraction
import org.json4s.JsonAST.JValue

import scala.concurrent.Future

trait ElasticSearchLocationWriteServer extends LocationWriteServer with ElasticSearchBaseWriteServer with LazyLogging {
  self : ElasticSearchBaseServer =>

  override def track(b: Breadcrumb): Future[Unit] = {
    val jv = Extraction.decompose(b)
    val key = b.productType+"."+b.productId+"."+b.location.time
    val latest = b.productType+"."+b.productId  // assuming they arrive in order, for this prototype ...

    for {
      path <- write("locations", key, jv).map { _ => () }
      recent <- write("latest", latest, jv).map { _ => () }
    } yield { () }
  }

  protected def write(index: String, key: String, r: JValue) : Future[ResourceId] = {
    val ukey = URLEncoder.encode(key, "UTF-8")
    put(index, ukey, r).map(_ => key)
  }
}
