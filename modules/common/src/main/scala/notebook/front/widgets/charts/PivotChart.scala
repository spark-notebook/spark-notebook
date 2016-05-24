package notebook.front.widgets.charts

import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.{Texts, Utils}
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

case class PivotChart[C:ToPoints:Sampler](
  originalData:C,
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = DEFAULT_MAX_POINTS,
  derivedAttributes:JsObject=play.api.libs.json.Json.obj(),
  options: Map[String, String] = Map.empty
) extends Chart[C](originalData, maxPoints) {
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

  protected def optionsJson = Json.obj(options.mapValues(Json.toJsFieldJsValueWrapper(_)).toSeq: _*)

  override val scripts = List(Script( "magic/pivotChart", Json.obj("width" → sizes._1,
                                                                   "height" → sizes._2,
                                                                   "derivedAttributes" → derivedAttributes,
                                                                   "extraOptions" → optionsJson)
  ))
}