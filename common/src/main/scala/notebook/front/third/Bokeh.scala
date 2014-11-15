package notebook.front.third

import io.continuum.bokeh._
import io.continuum.bokeh.Glyph
import notebook.front.Widget

import scala.xml.NodeSeq

/**
 * Created by gerrit on 15.11.14.
 */
class Bokeh(plotContext : PlotContext) extends Widget {
  override def toHtml: NodeSeq = {
    val writer = new HTMLFileWriter(List(plotContext), None)
    writer.renderPlots(writer.specs())(0)
  }
}

object Bokeh {
  def plot(plots : List[Plot]) = new Bokeh(new PlotContext().children(plots))


  case class ScatterPoint(x: Double, y: Double)

  def scatterPlot(points : Seq[ScatterPoint]) = {
    def buildPlotBase(source: DataSource) = {
      val xdr = new DataRange1d().sources(source.columns('x) :: Nil)
      val ydr = new DataRange1d().sources(source.columns('y) :: Nil)
      val plot = new Plot().x_range(xdr).y_range(ydr)
      plot
    }
    // Structure data
    val source = new ColumnDataSource()
      .addColumn('x, points.map(_.x))
      .addColumn('y, points.map(_.y))

    val plot = buildPlotBase(source)

    val glyphs = new Glyph()
      .data_source(source)
      .glyph(new Circle().x('x).y('y).radius(0.01).fill_color(Color.Aqua))

    val xaxis = new LinearAxis().plot(plot).location(Location.Below)
    val yaxis = new LinearAxis().plot(plot).location(Location.Left)

    plot.below <<= (xaxis :: _)
    plot.left <<= (yaxis :: _)

    plot.renderers := List(xaxis, yaxis, glyphs)
    Bokeh.plot(plot :: Nil)
  }
}

