/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */
package notebook

import scala.util.Try

import org.json4s.DefaultFormats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.slf4j.LoggerFactory

import notebook.util._

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

  implicit val ints = new Codec[JDouble, Int] {
    def decode(t: Int) = JDouble(t.toDouble)
    def encode(v: JDouble):Int = v.extract[Double].toInt
  }
  implicit val bools = new Codec[JValue, Boolean] {
    def decode(t: Boolean):JValue = JBool(t)
    def encode(v: JValue):Boolean = v match {
      case JString(s)  => s.toBoolean
      case JBool(b)    => b
      case x           => x.extract[Boolean]
    }
  }
  implicit val gints = new Codec[JValue, Int] {
    def decode(t: Int):JValue = JInt(t)
    def encode(v: JValue):Int = v match {
      case JString(s) => s.toInt
      case JInt(i)    => i.toInt
      case x          => x.extract[Int]
    }
  }
  implicit val longs = new Codec[JValue, Long] {
    def decode(t: Long):JValue = JInt(t)
    def encode(v: JValue):Long = v match {
      case JString(s) => s.toLong
      case JInt(i)    => i.toLong
      case x          => x.extract[Long]
    }
  }
  implicit val doubles = new Codec[JValue, Double] {
    def decode(t: Double) = JDouble(t)
    def encode(v: JValue):Double = v match {
      case JString(s)  => s.toDouble
      case JDouble(d)  => d
      case JDecimal(d) => d.toDouble
      case x           => x.extract[Double]
    }

  }
  implicit val floats = new Codec[JValue, Float] {
    def decode(t: Float):JValue = JDecimal(t)
    def encode(v: JValue):Float = v match {
      case JString(s)  => s.toFloat
      case JDouble(d)   => d.toFloat
      case JDecimal(d) => d.toFloat
      case x           => x.extract[Float]
    }
  }
  implicit val chars = new Codec[JValue, Char] {
    def decode(t: Char):JValue = JString(t.toString)
    def encode(v: JValue):Char = v match {
      case JString(s) => s.head
      case x          => x.extract[Char]
    }
  }
  implicit val strings = new Codec[JValue, String] {
    def decode(t: String):JValue = JString(t)
    def encode(v: JValue):String = v.extract[String]
  }
  implicit def defaultDates(implicit d:java.text.DateFormat) = new Codec[JValue, java.util.Date] {
    def decode(t: java.util.Date):JValue = JInt(t.getTime)
    def encode(v: JValue):java.util.Date = v match {
      case JString(s) => Try(s.toLong).toOption.map(x => new java.util.Date(x)).getOrElse(d.parse(s))
      case JInt(i)    => new java.util.Date(i.toLong)
      case x          => new java.util.Date(v.extract[Long])
    }
  }

  implicit val pair = new Codec[JValue, (Double,Double)] {
    def decode(t: (Double,Double)): JValue = Seq(t._1, t._2)
    def encode(v: JValue) = {
      val JArray(Seq(JDouble(x),JDouble(y))) = v
      (x, y)
    }
  }

  implicit def tMap[T](implicit codec:Codec[JValue, T]):Codec[JValue, Map[String, T]] = new Codec[JValue, Map[String, T]] {
    def encode(vs: JValue):Map[String, T] = {
      val JObject(xs) = vs
      xs.map { case JField(name, value) =>
        name -> codec.encode(value)
      }.toMap
    }
    def decode(ts: Map[String, T]): JValue = JObject( ts.map{ case (n, t) => JField(n, codec.decode(t))}.toList )
  }

  implicit def tSeq[T](implicit codec:Codec[JValue, T]):Codec[JValue, Seq[T]] = new Codec[JValue, Seq[T]] {
    def encode(vs: JValue) = for (JArray(Seq(t)) <- vs) yield codec.encode(t)
    def decode(ts: Seq[T]): JValue = ts.map(t => codec.decode(t))
  }

  implicit def idCodec[T]:Codec[T, T] = new Codec[T, T] {
    def encode(x: T): T = x
    def decode(x: T): T = x
  }


  implicit def jdouble2jvalueCodec[T](codec:Codec[JDouble, T]):Codec[JValue, T] = new Codec[JValue, T] {
    def encode(x:JValue):T = codec.encode(x)
    def decode(x:T):JValue = codec.decode(x)
  }

}
