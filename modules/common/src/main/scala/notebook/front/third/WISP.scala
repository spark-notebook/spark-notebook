package notebook.front.third
package wisp

import com.quantifind.charts.highcharts.{Series => HSeries, Data=>WData, _}
import notebook._
import notebook.front._
import play.api.libs.json._

trait SummarySeries {
  def data:Traversable[WData[_, _]]
  def transformData: WData[_, _] => WData[_, _]
  def transformSeries: HSeries => HSeries
  def chart: String

  lazy val series: HSeries = {
    val series = HSeries(data map transformData, chart = SeriesType.values.find(_ == chart) orElse (Some(chart)))
    transformSeries(series)
  }
}
case class Box[A: Numeric](source:Traversable[A], toBox: A => BoxplotData[A], transformSeries: HSeries => HSeries = identity[HSeries] _)
  extends SummarySeries {
  import Highchart._
  override val data:Traversable[WData[_, _]] = source map toBox
  override val chart = "boxplot"
  def transformData = identity[WData[_, _]] _
}
case class Pairs[A: Numeric, B: Numeric](source:Traversable[(A, B)], chart: String, updateData: DataPair[A, B] => DataPair[A, B] = identity[DataPair[A, B]] _, transformSeries: HSeries => HSeries = identity[HSeries] _)
  extends SummarySeries {
  import Highchart._
  override val data:Traversable[WData[_, _]] = source
  def transformData = (d:WData[_, _]) => updateData(d.asInstanceOf[DataPair[A, B]]).asInstanceOf[WData[_, _]]
}

case class Plot(override val data: Seq[SummarySeries],
  transform: Highchart => Highchart = identity[Highchart],
  xCat: Seq[Seq[String]] = Nil,
  yCat: Seq[Seq[String]] = Nil,
  sizes:(Int, Int)=(600, 400))
  extends JsWorld[SummarySeries, Highchart] {

  override val scripts: List[Script] = List(Script("wispWrap", JsObject(Seq(
    "width" →  JsNumber(sizes._1),
    "height" → JsNumber(sizes._2)
  ))))
  override val snippets: List[String] = Nil

  override lazy val toO: Seq[SummarySeries] => Seq[Highchart] =
    (ds: Seq[SummarySeries]) => {
      val h = transform(Highchart(ds.map(ss => ss.series)))
      Seq(
        h.copy(xAxis = Some(xCat.toArray.map(x => Axis(categories = Some(x.toArray)))).filter(!_.isEmpty))
         .copy(yAxis = Some(yCat.toArray.map(y => Axis(categories = Some(y.toArray)))).filter(!_.isEmpty))
      )
    }

  lazy val singleToO = ???

  implicit val singleCodec: Codec[JsValue, Highchart] = new Codec[JsValue, Highchart] {
    def encode(x: JsValue): Highchart = ???

    def decode(x: Highchart): JsValue = Json.parse(x.toJson)
  }
}

case class PlotH(h: Highchart,
  xCat: Option[Seq[String]] = None,
  yCat: Option[Seq[String]] = None,
  sizes:(Int, Int)=(600, 400))
  extends JsWorld[Highchart, Highchart] {

  override val data = Seq(h)

  override val scripts: List[Script] = List(Script("wispWrap", JsObject(Seq(
    "width" →  JsNumber(sizes._1),
    "height" → JsNumber(sizes._2)
  ))))
  override val snippets: List[String] = Nil

  override lazy val toO: Seq[Highchart] => Seq[Highchart] =
    (ds: Seq[Highchart]) => {
      val h = ds.head
      val x = xCat.map(
                x =>  h.copy(xAxis = Some(h.xAxis.getOrElse(Array.empty[Axis]) ++ Array(Axis(categories = Some(x.toArray)))))
              ).getOrElse(h)
      val y = yCat.map(
                y => h.copy(yAxis = Some(h.yAxis.getOrElse(Array.empty[Axis]) ++ Array(Axis(categories = Some(y.toArray)))))
              ).getOrElse(x)
      Seq(y)
    }

  lazy val singleToO = ???

  implicit val singleCodec: Codec[JsValue, Highchart] = new Codec[JsValue, Highchart] {
    def encode(x: JsValue): Highchart = ???

    def decode(x: Highchart): JsValue = Json.parse(x.toJson)
  }

}