package notebook.front.third

import io.continuum.bokeh.{Widget=>BWidget, _}
import io.continuum.bokeh.Glyph
import notebook._
import notebook.front._

import scala.xml.NodeSeq

import play.api.libs.json._

import com.quantifind.charts.repl.{IterableIterable, IterablePair, IterablePairLowerPriorityImplicits}
import com.quantifind.charts.highcharts.{Series=>HSeries, _ }
import com.quantifind.charts.Highcharts

object WISP {
  import Highchart._

  def toHighchart[A:Numeric, B:Numeric](data:Seq[(A,B)], chart:String):Highchart = {
    val highchart = chart match {
      case "area"       => Highchart(HSeries(data, chart = SeriesType.area))
      case "areaspline" => Highchart(HSeries(data, chart = SeriesType.areaspline))
      case "bar"        => Highchart(HSeries(data, chart = SeriesType.bar))
      case "column"     => Highchart(HSeries(data, chart = SeriesType.column))
      case "line"       => Highchart(HSeries(data, chart = SeriesType.line))
      case "pie"        => Highchart(HSeries(data, chart = SeriesType.pie))
      //case "regression" => Highchart(HSeries(data, chart = SeriesType.regression))
      case "scatter"    => Highchart(HSeries(data, chart = SeriesType.scatter))
      case "spline"     => Highchart(HSeries(data, chart = SeriesType.spline))
      //case _ => TODO
    }
    highchart
  }
}

class WISP[A:Numeric, B:Numeric](override val data: Seq[(A,B)], chart:String) extends JsWorld[(A,B), Highchart] {
  override val scripts: List[Script] = List(Script("wispWrap", JsObject(Nil)))
  override val snippets:List[String] = Nil

  override lazy val toO:Seq[(A,B)]=>Seq[Highchart] = (d:Seq[(A,B)])=>Seq(WISP.toHighchart(d, chart))
  lazy val singleToO = ???

  implicit val singleCodec:Codec[JsValue, Highchart] = new Codec[JsValue, Highchart] {
    def encode(x:JsValue):Highchart = ???
    def decode(x:Highchart):JsValue = Json.parse(x.toJson)
  }

}