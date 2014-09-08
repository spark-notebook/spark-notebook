/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
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
  val doubleSeq = new Codec[JValue, Seq[Double]] {
    def decode(t: Seq[Double]): JValue = t
    def encode(v: JValue) = for (JDouble(d) <- v) yield d
  }
  val pairSeq = new Codec[JValue, Seq[(Double,Double)]] {
    def decode(t: Seq[(Double,Double)]): JValue = for ((x,y) <- t) yield Seq(x,y)
    def encode(v: JValue) = for (JArray(Seq(JDouble(x),JDouble(y))) <- v) yield (x,y)
  }
  val pair = new Codec[JValue, (Double,Double)] {
    def decode(t: (Double,Double)): JValue = Seq(t._1, t._2)
    def encode(v: JValue) = {
      val JArray(Seq(JDouble(x),JDouble(y))) = v
      (x, y)
    }
  }
  val strings = new Codec[JValue, String] {
    def decode(t: String):JValue = JString(t)
    def encode(v: JValue) = v.extract[String]
  }

  def tSeq[T](implicit codec:Codec[JValue, T]) = new Codec[JValue, Seq[T]] {
    def decode(ts: Seq[T]): JValue = ts.map(t => codec.decode(t))
    def encode(vs: JValue) = for (JArray(Seq(t)) <- vs) yield codec.encode(t)
  }
}
