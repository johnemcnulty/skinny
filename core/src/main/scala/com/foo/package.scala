package com

import jdk.management.resource.ResourceType

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
package object foo {
  type ResourceId = String
  type ResourceType = String

  type DottedFieldName = String
  type FieldAccessRequirements = Map[DottedFieldName, AccessRequirement]
  type TypeAccessRequirements = Map[ResourceType, FieldAccessRequirements]

}
