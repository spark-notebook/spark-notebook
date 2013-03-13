/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import org.slf4j.LoggerFactory

/**
 * Author: Ken
 */


trait Codec[A,B] {
  def encode(x:A):B
  def decode(x:B):A
}

object JsonCodec {
  val log = LoggerFactory.getLogger(getClass())
  implicit val formats = DefaultFormats
  val ints = new Codec[JValue, Int] {
    def decode(t: Int) = JInt(t)
    def encode(v: JValue):Int = v.extract[Int]
  }
  val strings = new Codec[JValue, String] {
    def decode(t: String):JValue = {
      JString(t)
    }
    def encode(v: JValue) = v.extract[String]
  }
}
