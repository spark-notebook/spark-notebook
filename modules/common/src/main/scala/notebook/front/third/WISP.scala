package notebook.front.third
package wisp

import notebook._
import notebook.front._

import scala.xml.NodeSeq

import play.api.libs.json._

import com.quantifind.charts.repl.{IterableIterable, IterablePair, IterablePairLowerPriorityImplicits}
import com.quantifind.charts.highcharts.{Series=>HSeries, _ }
import com.quantifind.charts.Highcharts

case class SummarySeries[A:Numeric, B:Numeric](data:Seq[(A,B)], chart:String, f:HSeries=>HSeries=identity[HSeries] _) {
  import Highchart._
  lazy val series:HSeries = {
    val series = chart match {
      case "area"       => HSeries(data, chart = SeriesType.area)
      case "areaspline" => HSeries(data, chart = SeriesType.areaspline)
      case "bar"        => HSeries(data, chart = SeriesType.bar)
      case "column"     => HSeries(data, chart = SeriesType.column)
      case "line"       => HSeries(data, chart = SeriesType.line)
      case "pie"        => HSeries(data, chart = SeriesType.pie)
      //case "regression" => HSeries(data, chart = SeriesType.regression)
      case "scatter"    => HSeries(data, chart = SeriesType.scatter)
      case "spline"     => HSeries(data, chart = SeriesType.spline)
      //case _ => TODO
    }
    f(series)
  }
}

case class Plot[A:Numeric, B:Numeric](override val data: Seq[SummarySeries[A,B]], f:Highchart=>Highchart=identity[Highchart]) extends JsWorld[SummarySeries[A,B], Highchart] {
  import Highchart._
  override val scripts: List[Script] = List(Script("wispWrap", JsObject(Nil)))
  override val snippets:List[String] = Nil

  override lazy val toO:Seq[SummarySeries[A,B]]=>Seq[Highchart] =
    (ds:Seq[SummarySeries[A,B]]) =>
      Seq(f(Highchart(ds.map(ss => ss.series) )))

  lazy val singleToO = ???

  implicit val singleCodec:Codec[JsValue, Highchart] = new Codec[JsValue, Highchart] {
    def encode(x:JsValue):Highchart = ???
    def decode(x:Highchart):JsValue = Json.parse(x.toJson)
  }

}