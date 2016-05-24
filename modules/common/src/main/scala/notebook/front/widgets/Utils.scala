package notebook.front.widgets

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

import notebook.util.Reflector
import java.util.Date
import java.sql.{Date=>SqlDate}

import com.vividsolutions.jts.geom.Geometry
import org.wololo.geojson.GeoJSON

object Utils {
  object Defaults {
    val DEFAULT_MAX_POINTS = 1000
  }
}

trait Utils {

  def scopedScript(content: String, data: JsValue = null, selector:Option[String]=None) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    val withData = if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", Json.stringify(data), Null)

    val withSelector = selector.map { s =>
      withData % new UnprefixedAttribute("data-selector", s, Null)
    }.getOrElse(withData)
    withSelector
  }

  implicit val jsStringAnyCodec:Codec[JsValue, Seq[(String, Any)]] = new Codec[JsValue, Seq[(String, Any)]] {
    def decode(a: Seq[(String, Any)]):JsValue = JsObject(a.map( f => f._1.trim -> toJson(f._2) ))
    def encode(v: JsValue):Seq[(String, Any)] = ??? //todo
  }

  def toJson(obj: Any): JsValue = {
    obj match {
      case null             => JsNull
      case v: Int           => JsNumber(v)
      case v: Float         => JsNumber(v)
      case v: Double        => JsNumber(v)
      case v: Long          => JsNumber(v)
      case v: BigDecimal    => JsNumber(v)
      case v: String        => JsString(v)
      case v: Boolean       => JsBoolean(v)
      case v: Date          => JsNumber(v.getTime)
      case v: SqlDate       => JsNumber(v.getTime)
      case v: Geometry      =>
        val json  = geometryToGeoJSON(v)
        val jsonstring = json.toString()
        Json.parse(jsonstring)
      case v: GeoJSON       =>
        Json.parse(v.toString())
      case v: Iterable[_]   =>
        val it = v.map(x => toJson(x))
        JsArray(it.toSeq)
      case v:Tuple2[_,_]    =>
        JsObject(Seq(("_1" → toJson(v._1)), ("_2" → toJson(v._2))))
      case v:Tuple3[_,_,_]  =>
        JsObject(Seq(("_1" → toJson(v._1)), ("_2" → toJson(v._2)), ("_3" → toJson(v._3))))
      case v =>
        JsString(v.toString)
    }
  }

  def isNumber(obj: Any) = obj.isInstanceOf[Int] || obj.isInstanceOf[Float] || obj.isInstanceOf[Double] || obj.isInstanceOf[Long]
  def isDate(obj: Any) = obj.isInstanceOf[Date] || obj.isInstanceOf[SqlDate]

  def parseGeoJSON(s:String):GeoJSON = org.wololo.geojson.GeoJSONFactory.create(s)
  def geometryToGeoJSON(g:Geometry):GeoJSON = {
    import org.wololo.jts2geojson.GeoJSONWriter
    val writer = new GeoJSONWriter()
    val json:GeoJSON = writer.write(g)
    json
  }

}