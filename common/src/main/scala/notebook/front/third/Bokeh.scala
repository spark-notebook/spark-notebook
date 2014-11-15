package notebook.front.third

import java.util.UUID

import io.continuum.bokeh.{Plot, PlotContext, HTMLFileWriter, Document}
import notebook.front.Widget

import scala.xml.NodeSeq
import scala.xml.XML

/**
 * Created by gerrit on 15.11.14.
 */
class Bokeh(plotContext : PlotContext) extends Widget {
  override def toHtml: NodeSeq = {
    /*
    val content = scala.io.Source.fromFile(html.file)

                  .getLines()
                  .map(line => line.replace("\\\'", "\""))
                  .mkString("\n")
    XML.loadString(content).seq
    */
    new PlotContext().children()
    val writer = new HTMLFileWriter(List(plotContext), None)

    writer.renderPlots(writer.specs())(0)
  }
}

object Bokeh {
  def plot(plots : List[Plot]) = new Bokeh(new PlotContext().children(plots))
}

