package notebook

import play.api.libs.json._

import notebook._, JsonCodec._

case class Point(x:Double, y:Double)
case class Data(points:Seq[Point])
case class Series(name:String, color:String, data:Data)

object Series {
  implicit val pointCodec = new Codec[JsValue, Point] {
    val c = implicitly[Codec[JsValue, Map[String, Double]]]
    def encode(vs: JsValue):Point = ???
    def decode(p: Point): JsValue = c.decode(Map("x"->p.x, "y"->p.y))
  }

  implicit val dataCodec = new Codec[JsValue, Data] {
    val c = implicitly[Codec[JsValue, Seq[Point]]]
    def encode(vs: JsValue):Data = ???
    def decode(d: Data): JsValue = c.decode(d.points)
  }

  implicit val seriesCodec = new Codec[JsValue, Series] {
    def encode(vs: JsValue):Series = ???
    def decode(ts: Series): JsValue =
      Json.obj(
        "name" → JsString(ts.name),
        "color" → JsString(ts.color),
        "data" → implicitly[Codec[JsValue, Data]].decode(ts.data)
      )
  }

  def apply(name:String, color:String, data:Seq[(Double, Double)]):Series =
    Series(name, color, Data(data.map(d => Point(d._1, d._2))))
}