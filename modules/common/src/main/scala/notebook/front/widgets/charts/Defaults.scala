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

case class Tabs[C:ToPoints:Sampler](originalData:C, pages:Seq[(String, Chart[C])]=Nil, maxPoints:Int = DEFAULT_MAX_POINTS)
  extends JsWorld[Seq[(String, Any)], Seq[(String, Any)]]
  with Utils {
  import notebook.JSBus._

  // tabs widget itself send no reactive data-sets to browser.
  // for now, we send an empty dataset; it may be refactored further
  implicit val singleToO = identity[Seq[(String, Any)]] _
  implicit val singleCodec = jsStringAnyCodec
  override val data: Seq[Seq[(String, Any)]] = Seq()

  def newMax(max: Int) = pages foreach { case (_, w) => w.newMax(max) }

  override val scripts = List(
    Script("magic/tabs", Json.obj())
  )

  override def apply(newData: Seq[Seq[(String,Any)]]) {
    super.apply(newData)
    pages.foreach { case (s, w) =>
      w(newData)
    }
  }

  override val content = Some {
    <div>
      <div >
        <ul class="nav nav-tabs" id={ "ul"+id }>{
          pages.zipWithIndex map { p: ((String, Widget), Int) =>
            <li>
              <a href={ "#tab"+id+"-"+p._2 }><i class={ "fa fa-"+p._1._1} /></a>
            </li>
          }
        }</ul>

        <div class="tab-content" id={ "tab"+id }>{
          pages.zipWithIndex map { p: ((String, Widget), Int) =>
            <div class="tab-pane" id={ "tab"+id+"-"+p._2 }>
            { p._1._2 }
            </div>
          }
        }</div>
      </div>
    </div>
  }
}

case class ScatterChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C](originalData, maxPoints) with Sequencifiable[C] {

  override val scripts = List(Script( "magic/scatterChart",
                                      Json.obj( "x" → f1, "y" → f2,
                                                "width" → sizes._1, "height" → sizes._2)))
}

case class LineChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C](originalData, maxPoints) with Sequencifiable[C] {

  override val scripts = List(Script( "magic/lineChart",
                                      Json.obj( "x" → f1, "y" → f2,
                                                "width" → sizes._1, "height" → sizes._2)))
}

case class BarChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C](originalData, maxPoints) with Sequencifiable[C] {

  override val scripts = List(Script( "magic/barChart",
                                      Json.obj( "x" → f1, "y" → f2,
                                                "width" → sizes._1, "height" → sizes._2)))
}

case class PieChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C](originalData, maxPoints) with Sequencifiable[C] {

  override val scripts = List(Script( "magic/pieChart",
                                      Json.obj("series" → f1, "p" → f2,
                                                "width" → sizes._1, "height" → sizes._2)))
}

case class DiyChart[C:ToPoints:Sampler](originalData:C, js:String = "function(data, headers, chart) { console.log({'data': data, 'headers': headers, 'chart': chart}); }", override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C](originalData, maxPoints) {
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

  override val scripts = List(Script( "magic/diyChart",
                                      Json.obj( "js" → s"var js = $js;", "headers" → headers,
                                                "width" → sizes._1, "height" → sizes._2)))
}
