package notebook

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import notebook._, JsonCodec._

case class Point(x:Double, y:Double)
case class Data(points:Seq[Point])
case class Series(name:String, color:String, data:Data)

object Series {
  implicit val pointCodec = new Codec[JValue, Point] {
    val c = implicitly[Codec[JValue, Map[String, Double]]]
    def encode(vs: JValue):Point = ???
    def decode(p: Point): JValue = c.decode(Map("x"->p.x, "y"->p.y))
  }

  implicit val dataCodec = new Codec[JValue, Data] {
    val c = implicitly[Codec[JValue, Seq[Point]]]
    def encode(vs: JValue):Data = ???
    def decode(d: Data): JValue = c.decode(d.points)
  }

  implicit val seriesCodec = new Codec[JValue, Series] {
    def encode(vs: JValue):Series = ???
    def decode(ts: Series): JValue =
      JObject(List(
        JField("name", JString(ts.name)),
        JField("color", JString(ts.color)),
        JField("data", implicitly[Codec[JValue, Data]].decode(ts.data))
      ))
  }

  def apply(name:String, color:String, data:Seq[(Double, Double)]):Series =
    Series(name, color, Data(data.map(d => Point(d._1, d._2))))
}