package com.foo

import java.io.File

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods

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

object Validation {
  type ValidationError = JValue
}

import Validation._

class Validation(basedir: String) {

  protected val factory: JsonSchemaFactory = JsonSchemaFactory.byDefault()
  protected val validators = loadAll(basedir)

  def validate(jv: JValue) : List[ValidationError] = {
    (jv \ "productType") match {
      case JString(resourceType) if (validators.contains(resourceType)) =>
        import scala.collection.JavaConversions._
        val jn : JsonNode = JsonMethods.asJsonNode(jv)
        val report = validators(resourceType).validate(jn)
        if (report.isSuccess) Nil else {
          val messages = report.iterator()
          val errors = messages filter { message =>
            message.getLogLevel.toString == "error" }
          errors flatMap { ejn =>
            val jv = JsonMethods.fromJsonNode(ejn.asJson())
            (jv \ "reports") match {
              case JObject(fields) => fields.map(_._2) flatMap { issues => issues match {
                case JArray(vs) => vs.map(_ \ "message")
                case _ => Nil
              }}
              case _ => Nil
            }
          } toList;
        }
      case JString(resourceType) => JString(s"Unsupported productType $resourceType") :: Nil
      case _ => JString(s"productType attribute is missing") :: Nil
    }
  }

  protected def loadAll(dir: String) : Map[ResourceType, JsonSchema] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter { f => f.isFile && f.getName.endsWith(".schema.json") } .toList map { file =>
        val name = file.getName.dropRight(".schema.json".length)
        (name, factory.getJsonSchema(file.toURI.toString))
      } toMap;
    } else {
      Map.empty
    }
  }
}
