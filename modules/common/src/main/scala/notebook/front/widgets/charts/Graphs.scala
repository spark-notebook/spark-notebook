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

case class GraphChart[C:ToPoints:Sampler](originalData:C, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS, charge:Int= -30, linkDistance:Int=20, linkStrength:Double=1.0) extends Chart[C](originalData, maxPoints) {
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq


  val opts = Json.obj("headers" → headers, "width" → sizes._1,
                      "height" → sizes._2, "charge" → charge, "linkDistance" → linkDistance,
                      "linkStrength" → linkStrength)

  override val scripts = List(Script("magic/graphChart", opts))
}
