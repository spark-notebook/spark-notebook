/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json.JsonAST.{JString, JField, JObject}

/**
 * Pluggable interface for completing Strings.
 */

case class Match(matchedValue: String, metadata: Map[String, String]) {
  def toJson = JObject(JField("completion", JString(matchedValue)) :: (metadata.map { case (name, value) => JField(name, JString(value)) } toList))
}

object Match {
  def apply(matchedValue: String): Match = Match(matchedValue, Map())
}

trait StringCompletor {
  def complete(stringToComplete: String): (String, Seq[Match])
}
