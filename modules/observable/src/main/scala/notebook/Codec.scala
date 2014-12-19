/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */
package notebook

import scala.util.Try

import play.api.libs.json._
import play.api.libs.functional.syntax._

import org.slf4j.LoggerFactory

import notebook.util._

trait Codec[A,B] {
  def encode(x:A):B
  def decode(x:B):A
}

object JsonCodec {
  val log = LoggerFactory.getLogger(getClass())

  implicit def formatToCodec[A](implicit f:Format[A]):Codec[JsValue, A] = new Codec[JsValue, A] {
    def decode(a: A):JsValue = f.writes(a)
    def encode(v: JsValue):A = f.reads(v) match {
      case s: JsSuccess[A] => s.get
      case e: JsError => throw new RuntimeException("Errors: " + JsError.toFlatJson(e).toString())
    }
  }

  implicit val ints:Codec[JsValue, Int] = formatToCodec(Format.of[Int])
  implicit val longs:Codec[JsValue, Long] = formatToCodec(Format.of[Long])
  implicit val doubles:Codec[JsValue, Double] = formatToCodec(Format.of[Double])
  implicit val floats:Codec[JsValue, Float] = formatToCodec(Format.of[Float])
  implicit val strings:Codec[JsValue, String] = formatToCodec(Format.of[String])
  implicit val chars:Codec[JsValue, Char] = formatToCodec(Format(Reads.of[String].map(_.head), Writes((c:Char) => JsString(c.toString))))
  implicit val bools:Codec[JsValue, Boolean] = formatToCodec(Format.of[Boolean])

  implicit def defaultDates(implicit d:java.text.DateFormat) = new Codec[JsValue, java.util.Date] {
    def decode(t: java.util.Date):JsValue = JsNumber(t.getTime)
    def encode(v: JsValue):java.util.Date = v match {
      case JsString(s)  => Try(s.toLong).toOption.map(x => new java.util.Date(x)).getOrElse(d.parse(s))
      case JsNumber(i) => new java.util.Date(i.toLong)
      case x           => new java.util.Date(v.as[Long])
    }
  }
/*  implicit val ints = new Codec[JsDouble, Int] {
    def decode(t: Int) = JsDouble(t.toDouble)
    def encode(v: JsDouble):Int = v.extract[Double].toInt
  }
  implicit val bools = new Codec[JsValue, Boolean] {
    def decode(t: Boolean):JsValue = JBool(t)
    def encode(v: JsValue):Boolean = v match {
      case JString(s)  => s.toBoolean
      case JBool(b)    => b
      case x           => x.extract[Boolean]
    }
  }
  implicit val gints = new Codec[JsValue, Int] {
    def decode(t: Int):JsValue = JInt(t)
    def encode(v: JsValue):Int = v match {
      case JString(s) => s.toInt
      case JInt(i)    => i.toInt
      case x          => x.extract[Int]
    }
  }
  implicit val longs = new Codec[JsValue, Long] {
    def decode(t: Long):JsValue = JInt(t)
    def encode(v: JsValue):Long = v match {
      case JString(s) => s.toLong
      case JInt(i)    => i.toLong
      case x          => x.extract[Long]
    }
  }
  implicit val doubles = new Codec[JsValue, Double] {
    def decode(t: Double) = JDouble(t)
    def encode(v: JsValue):Double = v match {
      case JString(s)  => s.toDouble
      case JDouble(d)  => d
      case JDecimal(d) => d.toDouble
      case x           => x.extract[Double]
    }

  }
  implicit val floats = new Codec[JsValue, Float] {
    def decode(t: Float):JsValue = JDecimal(t)
    def encode(v: JsValue):Float = v match {
      case JString(s)  => s.toFloat
      case JDouble(d)   => d.toFloat
      case JDecimal(d) => d.toFloat
      case x           => x.extract[Float]
    }
  }
  implicit val chars = new Codec[JsValue, Char] {
    def decode(t: Char):JsValue = JString(t.toString)
    def encode(v: JsValue):Char = v match {
      case JString(s) => s.head
      case x          => x.extract[Char]
    }
  }
  implicit val strings = new Codec[JsValue, String] {
    def decode(t: String):JsValue = JString(t)
    def encode(v: JsValue):String = v.extract[String]
  }
  implicit def defaultDates(implicit d:java.text.DateFormat) = new Codec[JsValue, java.util.Date] {
    def decode(t: java.util.Date):JsValue = JInt(t.getTime)
    def encode(v: JsValue):java.util.Date = v match {
      case JString(s) => Try(s.toLong).toOption.map(x => new java.util.Date(x)).getOrElse(d.parse(s))
      case JInt(i)    => new java.util.Date(i.toLong)
      case x          => new java.util.Date(v.extract[Long])
    }
  }*/

  implicit val pair = new Codec[JsValue, (Double,Double)] {
    def decode(t: (Double,Double)): JsValue = Json.arr(t._1, t._2)
    def encode(v: JsValue) = {
      val Seq(JsNumber(x), JsNumber(y)) = v.asInstanceOf[JsArray].value
      (x.toDouble, y.toDouble)
    }
  }

  implicit def tMap[T](implicit codec:Codec[JsValue, T]):Codec[JsValue, Map[String, T]] = new Codec[JsValue, Map[String, T]] {
    def encode(vs: JsValue):Map[String, T] = {
      val jo:JsObject = vs.asInstanceOf[JsObject]
      jo.value.map { case (name, jsValue) =>
        name -> codec.encode(jsValue)
      }.toMap
    }
    def decode(ts: Map[String, T]): JsValue = JsObject( ts.map{ case (n, t) => (n, codec.decode(t))}.toSeq )
  }

  implicit def tSeq[T](implicit codec:Codec[JsValue, T]):Codec[JsValue, Seq[T]] = new Codec[JsValue, Seq[T]] {
    def encode(vs: JsValue) = vs.asInstanceOf[JsArray].value.map(codec.encode _)

    def decode(ts: Seq[T]): JsValue = JsArray(ts.map(t => codec.decode(t)))
  }

  /*
  implicit def jdouble2jsvalueCodec[T](codec:Codec[JDouble, T]):Codec[JsValue, T] = new Codec[JsValue, T] {
    def encode(x:JsValue):T = codec.encode(x)
    def decode(x:T):JsValue = codec.decode(x)
  }*/

  implicit def idCodec[T]:Codec[T, T] = new Codec[T, T] {
    def encode(x: T): T = x
    def decode(x: T): T = x
  }


}
