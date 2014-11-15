package notebook.front.third

import java.util.UUID

import io.continuum.bokeh.Document
import notebook.front.Widget

import scala.xml.NodeSeq
import scala.xml.XML

/**
 * Created by gerrit on 15.11.14.
 */
class Bokeh(bokehDoc: Document) extends Widget {
  private val path: String = UUID.randomUUID().toString + ".html"

  lazy val html = bokehDoc.save(path)

  override def toHtml: NodeSeq = {
    /*
    val content = scala.io.Source.fromFile(html.file)

                  .getLines()
                  .map(line => line.replace("\\\'", "\""))
                  .mkString("\n")
    XML.loadString(content).seq
    */
    XML.loadFile(html.file).seq
  }
}

object Bokeh {
  def document(bokehDoc: Document) = new Bokeh(bokehDoc)
}

