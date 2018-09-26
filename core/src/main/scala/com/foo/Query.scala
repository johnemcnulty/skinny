package com.foo

import com.foo.product.BadRequestException

import scala.util.{Failure, Success, Try}

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


object Query {

  val empty = Query()

  trait StringExtractable[T] {
    def fromString(s: String): Try[T]
  }

  object StringExtractable {

    implicit object IntAreExtractable extends StringExtractable[Int] {
      override def fromString(s: String): Try[Int] = Try(s.toInt)
    }

    implicit object BooleansAreExtractable extends StringExtractable[Boolean] {
      override def fromString(s: String): Try[Boolean] = Try(s.toBoolean)
    }

    implicit object StringsAreExtractable extends StringExtractable[String] {
      override def fromString(s: String): Try[String] = Try(s)
    }

  }

  case class Parameter[T](name: String, allowed: T => Boolean, required: Boolean = false)(implicit ev: StringExtractable[T]) {
    def extract(params: Map[String, String]): Either[String, Option[T]] = {
      params.get(name) match {
        case Some(value) => ev.fromString(value) match {
          case Success(v) if allowed(v) =>
            Right(Some(v))
          case Success(v) => Left(s"Invalid value for $name: $v")
          case Failure(t) => Left(s"Invalid value for $name: $t")
        }
        case None if required => Left[String, Option[T]](s"Missing required parameter: $name")
        case None => Right[String, Option[T]](None)
      }
    }
  }

  def extractQuery(params: Map[String, String]): Try[Query] = {
    val sortBy = Parameter[String]("sortBy", (t: String) => true)
    val sortDir = Parameter[String]("sortDir", (t: String) => Set("asc", "desc").contains(t.toLowerCase))
    val page = Parameter[Int]("page[number]", (t: Int) => t >= 0)
    val limit = Parameter[Int]("page[size]", (t: Int) => t >= 0)

    val pdefs = sortBy :: sortDir :: page :: limit :: Nil
    val bad = pdefs
      .map { p => (p.name, p.extract(params)) }
      .collect { case (name, Left(msg)) => (name, msg) }

    if (bad.size > 0) {
      Failure(new BadRequestException(bad.toMap))
    } else {
      val names = pdefs.map(_.name)
      val remaining = params.filterNot { case (name, _) => names.contains(name) }

      Success(Query(remaining,
        sortBy.extract(params).right.get,
        sortDir.extract(params).right.get,
        page.extract(params).right.get.getOrElse(0),
        limit.extract(params).right.get.getOrElse(10)
      ))
    }
  }
}

case class Query(query: Map[String, String] = Map(), sortBy: Option[String] = None, sortDir: Option[String] = None, pageNum: Int = 0, pageSize: Int = 10) {
  override def toString: ResourceId = {
    val constraints = (query map { case (k, v) => s"$k=$v" }).mkString(",")
    val sort = if (sortBy.isDefined) s"sortBy=${sortBy.get}, " else ""
    val dir  = if (sortDir.isDefined) s"sortDir=${sortDir.get}, " else ""
    s"$constraints, ${sort}${dir}page[number]=$pageNum, page[size]=$pageSize"
  }
}
