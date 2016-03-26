package notebook.front.widgets.charts

import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.{Texts, Utils}

import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

case class TableChart[C:ToPoints:Sampler](
  originalData:C,
  filterCol:Option[Seq[String]]=None,
  override val sizes:(Int, Int)=(600, 400),
  maxPoints:Int = Utils.Defaults.DEFAULT_MAX_POINTS
) extends Chart[C](originalData, maxPoints) {
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
    t.data.toSeq.filter{case (k, v) => filterCol.getOrElse(headers).contains(k)}
  }
  val h:Seq[String] = filterCol.getOrElse(headers)
  override val scripts = List(Script( "magic/tableChart",
                                      Json.obj( "headers" → h,
                                                "width" → sizes._1, "height" → sizes._2)))
}