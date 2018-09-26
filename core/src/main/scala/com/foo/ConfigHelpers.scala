package com.foo

import java.time.temporal.ChronoUnit

import com.typesafe.config.Config

import scala.concurrent.duration.{FiniteDuration, SECONDS}
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


trait ConfigHelpers {

  def getDuration(path: String, fallback: FiniteDuration)(implicit config: Config) : FiniteDuration = {
    if (config.hasPath(path)) {
      Try (FiniteDuration(config.getDuration(path).get(ChronoUnit.SECONDS), SECONDS)) match {
        case Success(d) => d
        case Failure(t) => fallback
      }
    } else {
      fallback
    }
  }

}
