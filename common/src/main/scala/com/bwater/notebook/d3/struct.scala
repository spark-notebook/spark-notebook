package com.bwater.notebook.widgets.d3

import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._
import com.bwater.notebook._, JsonCodec._

case class Series(name:String, color:String, data:Seq[Map[String, Double]])

object Series {
  implicit val seriesCodec = new Codec[JValue, Series] {
    def encode(vs: JValue):Series = ???
    def decode(ts: Series): JValue = 
      JObject(List(
        JField("name", JString(ts.name)), 
        JField("color", JString(ts.color)), 
        JField("data", implicitly[Codec[JValue, Seq[Map[String, Double]]]].decode(ts.data))
      ))
  }
}
