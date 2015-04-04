package notebook.front.third

import org.viz.lightning.Visualization

import scala.xml.NodeSeq

object LightningViz {

  def show(data: Visualization): NodeSeq = {
    <iframe src={data.getIframeLink} width="100%" class="lightning" height="600"></iframe>
  }

}