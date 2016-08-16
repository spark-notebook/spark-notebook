package notebook.front.widgets.charts
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.Utils
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

case class CustomPlotlyChart[C:ToPoints:Sampler](
  originalData: C,
  layout: String = "{}",
  dataOptions: String = "{}",
  dataSources: String = "{}",
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = DEFAULT_MAX_POINTS
) extends Chart[C](originalData, maxPoints) {

  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

  override val scripts = List(Script( "magic/customPlotlyChart",
    Json.obj(
      "js" → s"var layout = $layout; var dataSources=$dataSources; var dataOptions = $dataOptions",
      "headers" → headers,
      "height" → sizes._2)))
}