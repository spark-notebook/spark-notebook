package notebook.front.widgets.charts

import play.api.libs.json._
import com.vividsolutions.jts.geom.Geometry
import org.wololo.geojson.GeoJSON

import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.Utils
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

case class GeoPointsChart[C:ToPoints:Sampler](
  originalData:C,
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = DEFAULT_MAX_POINTS,
  latLonFields:Option[(String, String)]=None,
  rField:Option[String]=None,
  colorField:Option[String]=None) extends Chart[C](originalData, maxPoints) {

  val latLong = latLonFields.getOrElse((headers(0), headers(1)))

  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
    val stripedData = t.data.toSeq.filter { case (k, v) =>
                                            k == latLong._1 ||
                                            k == latLong._2 ||
                                            Some(k) == rField ||
                                            Some(k) == colorField
                                          }
    stripedData
  }

  override val scripts =
    List(Script("magic/geoPointsChart",
      Json.obj(
                "lat" → latLong._1, "lon" → latLong._2,
                "width" → sizes._1, "height" → sizes._2,
                "rField" → rField, "colorField" → colorField
                /*, "proj" → proj, "baseMap" → baseMap*/
              )
      ++ rField.map(r => Json.obj("r" → r)).getOrElse(Json.obj())
      ++ colorField.map(color => Json.obj("color" → color)).getOrElse(Json.obj())
    ))
}

case class GeoChart[C:ToPoints:Sampler](
  originalData:C,
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = DEFAULT_MAX_POINTS,
  geometryField:Option[String]=None,
  rField:Option[String]=None,
  colorField:Option[String]=None,
  fillColorField:Option[String]=None) extends Chart[C](originalData, maxPoints) {

  val geometry = geometryField.getOrElse(headers(0))

  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
    val stripedData = t.data.toSeq.filter { case (k, v) =>
                                            k == geometry ||
                                            Some(k) == rField ||
                                            Some(k) == colorField  ||
                                            Some(k) == fillColorField
                                          }
    stripedData
  }

  override val scripts =
    List(Script("magic/geoChart",
      Json.obj(
        "geometry" → geometry,
        "width" → sizes._1, "height" → sizes._2
      )
      ++ rField.map(r => Json.obj("r" → r)).getOrElse(Json.obj())
      ++ colorField.map(color => Json.obj("color" → color)).getOrElse(Json.obj())
      ++ fillColorField.map(color => Json.obj("fillColor" → color)).getOrElse(Json.obj())
    ))
}