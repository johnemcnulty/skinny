package com.foo.elastic

import com.foo.{DottedFieldName, FieldAccessRequirements}
import org.json4s.JValue

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



class ElasticSearchQueryBuilder(freqs: FieldAccessRequirements) {

  import org.json4s.JsonDSL.WithDouble._

  case class AccessRequirementsException(msg: String) extends Exception(msg)

  def toJson(filters: Map[DottedFieldName, String])() : Try[JValue] = {

    /*
     * In the current version of access requirements to mappings (2.1.3 of bp-stash),
     * (1) all searches are done against the base field which will either be case sensitive or not
     * (2) substring searches are implemented as a wild card search
     */
    val (substr, exact) = filters partition  { case (_, v) => v.startsWith("(") && v.endsWith(")") }

    val unsupportedExact = exact.keys filter { k => freqs.contains(k) == false || freqs(k).exactMatch == false }
    val unsupportedSubstr = substr.keys filter { k => freqs.contains(k) == false || freqs(k).substringMatch == false }

    val errors = unsupportedExact.map { k => s"Exact matches not supported for $k" } ++
      unsupportedSubstr.map { k => s"Substring search not supported for: $k" }

    errors.size match {
      case 0 =>
        val exactTerms = exact map { case (k, v) =>
          val searchType = if (freqs.get(k).isDefined && freqs(k).caseInsensitiveMatch) "match" else "term"
          val jv: JValue = (searchType -> (k -> v)); jv
        }
        val substrTerms = substr map { case (k, v) =>
          //val jv : JValue = ("match" -> (k -> v.drop(1).dropRight(1))); jv
          val content = v.drop(1).dropRight(1)
          val norm = if (freqs.get(k).isDefined && freqs(k).caseInsensitiveMatch) content.toLowerCase else content
          val value = "*" + norm + "*"
          val jv : JValue = ("wildcard" -> (k -> value)); jv
        }

        val terms = exactTerms ++ substrTerms

        terms.size match {
          case 1 =>
            val query : JValue = ("query" -> ("constant_score" -> ("filter" -> terms.head)))
            Success(query)
          case _ =>
            val query : JValue = ("query" -> ("constant_score" -> ("filter" -> ("bool" -> ("must" -> terms)))))
            Success(query)
        }
      case _ => Failure(new AccessRequirementsException(errors.mkString("\n")))
    }
  }
}
