package notebook.front.third

import io.continuum.bokeh.{Widget=>BWidget, _}
import io.continuum.bokeh.Glyph
import notebook._
import notebook.front._

import scala.xml.NodeSeq

import play.api.libs.json._

import com.quantifind.charts.repl.{IterableIterable, IterablePair, IterablePairLowerPriorityImplicits}
import com.quantifind.charts.highcharts._
import com.quantifind.charts.Highcharts

object WISP {
  def toHighchart[A:Numeric, B:Numeric](data:Seq[(A,B)], chart:String):Highchart = {
    val highchart = chart match {
      case "area"       => Highcharts.area(data)
      case "areaspline" => Highcharts.areaspline(data)
      case "bar"        => Highcharts.bar(data)
      case "column"     => Highcharts.column(data)
      case "line"       => Highcharts.line(data)
      case "pie"        => Highcharts.pie(data)
      case "regression" => Highcharts.regression(data)
      case "scatter"    => Highcharts.scatter(data)
      case "spline"     => Highcharts.spline(data)
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
    def decode(x:Highchart):JsValue = Json.toJson(x.toJson)
  }

}