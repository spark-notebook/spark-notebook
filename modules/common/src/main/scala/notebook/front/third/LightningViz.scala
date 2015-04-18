package notebook.front.third

import notebook.front.Widget
import notebook.front.widgets._
import org.viz.lightning.Visualization
import play.api.libs.json.Json

import scala.xml.NodeSeq

object LightningViz {

  def show(data: Visualization): LightningViz = {
    new LightningViz(data)
  }

}

class LightningViz(data: Visualization) extends Widget {

  lazy val toHtml = <div id={"pymIframe" + data.vid}>{
    scopedScript(
    """
      |req( ['pym'],
      |  function (pym) {
      |   var pymIframe = new pym.Parent('pymIframe'+vid, pymLink, {})
      |  }
      |);""".stripMargin,
      Json.obj("pymLink" -> data.getPymLink, "vid" -> data.vid)
    )
  }</div>

}