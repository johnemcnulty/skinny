package com.foo.location

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

case class Location(lat: Float, lon: Float, time: String)
case class Breadcrumb(productType: String, productId: String, location: Location)
case class Path(productType: String, productId: String, locations: Seq[Location])

trait LocationReadServer {
  def location(productType: String, productId: String) : Future[Try[Breadcrumb]]
  def history(productType: String, productId: String) : Future[Try[Path]]
}
trait LocationWriteServer {
  def track(b: Breadcrumb) : Future[Unit]
}
trait LocationServer extends LocationReadServer with LocationWriteServer

