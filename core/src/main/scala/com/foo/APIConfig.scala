package com.foo

import com.typesafe.config.Config

import scala.concurrent.duration._

object APIConfig extends ConfigHelpers {
  def apply(config: Config) : APIConfig = {
    implicit val c = config
    val gt = getDuration("get-timeout", 5 seconds)
    val qt = getDuration("query-timeout", 5 seconds)
    APIConfig(gt, qt)
  }
}

case class APIConfig(getTimeout: FiniteDuration = 2 seconds,
                     queryTimeout: FiniteDuration = 5 seconds) // does the API path require a trailing slash?
