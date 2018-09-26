package com.foo.product

import java.util.concurrent.CountDownLatch

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentType, ContentTypes, MediaTypes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.foo.{APIHelpers, ResourceId, ResourceType}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.JValue
import org.json4s.JsonAST.JInt
import org.json4s.jackson.JsonMethods
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.ContentNegotiator.Alternative.MediaType

import scala.concurrent.Future

class ProductAPISpec
  extends FlatSpec with Matchers
    with ScalatestRouteTest with APIHelpers
    with BeforeAndAfter with BeforeAndAfterAll with SuiteMixin
    with ScalaFutures
    with LazyLogging {

  val latch = new CountDownLatch(expectedTestCount(Filter.default))

  override def withFixture(test: NoArgTest) = {
    logger.info(s"========= ${test.name} =========")
    super.withFixture(test)
  }


  val productTypes = IndexedSeq("fish", "cheese", "coffee", "wine")
  def result(id: String) : JValue = {
    import org.json4s.JsonDSL._
    ("id" -> id) ~ ("productType" -> productTypes(id.toInt % productTypes.size))
  }

  def server(probe: ActorRef) = new ProductServer {

    override def createOrUpdate(jv: JValue): Future[Response[ResourceId]] = { probe ! jv; Future.successful(RequestSucceeded("0")) }

    override def lookup(resourceType: ResourceType, id: ResourceId): Future[Response[JValue]] =  {
      probe ! s"LOOKUP:$resourceType:$id"
      Future.successful(RequestSucceeded(JInt(0)))
    }

    override def query(productType: ResourceType, parameters: Map[String, String], sortBy: Option[String], sortDir: Option[String], pageNum: Int, pageSize: Int): Future[Response[QueryResults]] = {
      probe ! (productType, parameters, sortBy, sortDir, pageNum, pageSize)
      Future.successful(NotFound("0"))
    }

    override def delete(resourceType: ResourceType, id: ResourceId): Future[Response[Unit]] = {
      probe ! s"DELETE:$resourceType:$id"
      Future.successful(RequestSucceeded(()))
    }
  }

  "ProductAPI" should "call server lookup with productType/id for a GET on /path/productType/id" in {
    try {
      val probe = TestProbe()
      val api = ProductAPI("/hello", server(probe.ref), Map.empty)(system.dispatcher).route

      Get("/hello/coffee/2") ~> api
      probe.expectMsg("LOOKUP:coffee:2")
    } finally {
      latch.countDown()
    }
  }

  it should "call server delete with productType/id for a DELETE on /path/productType/id" in {
    try {
      val probe = TestProbe()
      val api = ProductAPI("/hello", server(probe.ref), Map.empty)(system.dispatcher).route

      Delete("/hello/coffee/2") ~> api
      probe.expectMsg("DELETE:coffee:2")
    } finally {
      latch.countDown()
    }
  }

  it should "call server query with parameters for a GET on /path/productType" in {
    try {
      val probe = TestProbe()
      val api = ProductAPI("/hello", server(probe.ref), Map.empty)(system.dispatcher).route
      Get("/hello/wine?a=1&b=2") ~> api

      probe.expectMsg(("wine", Map("a" -> "1", "b" -> "2"), None, None, 0, 10))
    } finally {
      latch.countDown()
    }
  }

  override def afterAll = {
    latch.await()
  }
}
