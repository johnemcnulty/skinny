package com.foo.elastic.product

import java.net.URLEncoder

import com.foo.elastic.{ElasticSearchBaseServer, ElasticSearchBaseWriteServer}
import com.foo.product.{InternalServerError, ProductWriteServer, RequestSucceeded, Response}
import com.foo.{ResourceId, ResourceType}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.JsonAST.JValue

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait ElasticSearchWriteServer extends ProductWriteServer with ElasticSearchBaseWriteServer with LazyLogging {
  self : ElasticSearchBaseServer =>

  override def createOrUpdate(jv: JValue): Future[Response[ResourceId]] = {
    write(jv).map { rid => RequestSucceeded(rid) }
  }

  protected def write(r: JValue) : Future[ResourceId] = {
    val id = (r \ "id").extract[ResourceId]
    val rt = (r \ "productType").extract[ResourceType]
    val ukey = URLEncoder.encode(id, "UTF-8")

    put(rt, ukey, r).map(_ => id)
  }

  override def delete(resourceType: ResourceType, id: ResourceId): Future[Response[Unit]] = {
    del(resourceType, id).map(x => x match {
      case Success(_) => RequestSucceeded(())
      case Failure(t) => InternalServerError(t)
    })
  }
}
