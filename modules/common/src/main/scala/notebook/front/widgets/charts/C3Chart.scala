package notebook.front.widgets.charts

import play.api.libs.json._
import notebook.front.Script
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

case class CustomC3Chart[C:ToPoints:Sampler](
  originalData:C,
  chartOptions :String = "{}",
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = DEFAULT_MAX_POINTS
) extends Chart[C](originalData, maxPoints) {

  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

  override val scripts = List(Script( "magic/customC3Chart",
    Json.obj(
      "js" → s"var chartOptions = $chartOptions;",
      "headers" → headers,
      "height" → sizes._2)))
}
