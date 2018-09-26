package com.foo.product

import com.foo.{ResourceId, ResourceType}
import org.json4s.JsonAST.JValue

import scala.concurrent.Future
import scala.util.Try

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


trait Response[T]

case class QueryResults(totalMatching: Int, page: Int, limit: Int, data: Iterable[JValue])

case class RequestSucceeded[T](result: T) extends Response[T]

trait RequestFailed[T] extends Response[T]

case class RequestError(desc: JValue)
case class BadRequest[T](errors: List[RequestError]) extends RequestFailed[T]
case class InternalServerError[T](t: Throwable) extends RequestFailed[T]
case class NotFound[T](id: ResourceId) extends RequestFailed[T]

trait ProductReadServer {
  def lookup(resourceType: ResourceType, id: ResourceId) : Future[Response[JValue]]
  def query(productType: ResourceType, parameters: Map[String, String], sortBy: Option[String] = None, sortDir: Option[String] = None, pageNum: Int = 0, pageSize: Int = 10) : Future[Response[QueryResults]]
}

trait ProductWriteServer {
  def createOrUpdate(jv: JValue) : Future[Response[ResourceId]]
  def delete(resourceType: ResourceType, id: ResourceId) : Future[Response[Unit]]
}

trait ProductServer extends ProductReadServer with ProductWriteServer

