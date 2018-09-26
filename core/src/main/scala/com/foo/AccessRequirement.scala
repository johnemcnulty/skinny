package com.foo

import com.foo.AccessRequirement.DottedName

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

object JsonSchemaTypes extends Enumeration {
  type JsonSchemaType = Value
  val `null`, `string`, `boolean`, `number`, `object`, `array` = Value
}

object AccessRequirement {
  type DottedName = String 
}

import AccessRequirement._

case class AccessRequirement(fieldName: DottedName,
                             accessType: JsonSchemaTypes.JsonSchemaType,
                             exactMatch: Boolean = true,
                             substringMatch: Boolean = false,
                             caseInsensitiveMatch: Boolean = false,
                             sortBy: Boolean = false) {

  def isInnerField : Boolean = fieldName.contains("\\.")

  // if either requirement has a tighter restriction, use the tighter one
  def +(other: AccessRequirement): AccessRequirement =
    ((fieldName == other.fieldName)) match {
      case true if accessType == other.accessType =>
        AccessRequirement(fieldName, accessType,
          exactMatch || other.exactMatch,
          substringMatch || other.substringMatch,
          caseInsensitiveMatch || other.caseInsensitiveMatch,
          sortBy || other.sortBy
        )
      case true => throw new Exception(s"AccessRequirements disagree on type of ${fieldName}: ${accessType} vs ${other.accessType}")
      case false => this
    }
}

