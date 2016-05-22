package notebook.front.widgets.charts

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.{Texts, Utils}
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

abstract class DataToRenderableConverter[C: ToPoints : Sampler](originalData: C, maxPoints: Int)
  extends JsWorld[Seq[(String, Any)], Seq[(String, Any)]] {
  // conversion from any renderable format (List, Array, DataFrame),
  // into a generic Seq of items (Seq[MagicRenderPoint])
  def sampler = implicitly[Sampler[C]]
  def toPoints = implicitly[ToPoints[C]]
  lazy val initialItems: Seq[MagicRenderPoint] = toPoints(originalData, maxPoints)

  // conversion into data format passed into javascript widgets (via observable)
  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)]
  def computeData(pts:Seq[MagicRenderPoint]) = pts.map(mToSeq)

  // initial items to be displayed in JS (and later saved in notebook)
  lazy val data: Seq[Seq[(String, Any)]] = computeData(initialItems)

  lazy val headers = toPoints.headers(originalData)
  lazy val numOfFields = headers.size
}

abstract class Chart[C:ToPoints:Sampler](originalData: C, maxPoints: Int)
  extends DataToRenderableConverter[C](originalData, maxPoints)
  with JsWorld[Seq[(String, Any)], Seq[(String, Any)]]
  with Texts
  with Utils {
  import notebook.JSBus._

  def sizes:(Int, Int)=(600, 400)

  @volatile var currentC = originalData
  @volatile var currentPoints = initialItems
  @volatile var currentMax = maxPoints

  def approxTotalItemCount(): String = {
    sampler.samplingStrategy match {
      // on DataFrames, do not call df.count() as it's rather expensive
      case magic.LimitBasedSampling() =>
        val sampledCount = currentPoints.length
        if (currentMax > sampledCount) s"$sampledCount" else s"$sampledCount or more"
      case _ => s"${toPoints.count(currentC)}"
    }
  }

  def samplingWarningMsg(): String = {
    sampler.samplingStrategy match {
        case magic.LimitBasedSampling() =>
          if (currentMax > currentPoints.length) ""
          else " (Warning: showing only first " + currentMax + " rows)"

        case _ if currentMax <= toPoints.count(currentC) =>
          " (Warning: randomly sampled "+currentMax + " entries)"

        case _ => ""
    }
  }

  // initialize sampling warning on Chart initialization
  val totalRowCount = outWithInitialValue(approxTotalItemCount)
  val warnSamplingInUse = outWithInitialValue(samplingWarningMsg)

  // ---- Helpers to mutate the chart reactively ----
  // ------------------------------------------------
  def updateChartStatus() = {
    warnSamplingInUse(samplingWarningMsg)
    totalRowCount(approxTotalItemCount)
  }

  def newMax(max:Int) = {
    //update state
    currentMax = max
    applyOn(currentC)
  }

  def applyOn(newData:C) = apply {
    currentC = newData
    currentPoints = toPoints(newData, currentMax)
    val d = currentPoints map mToSeq
    updateChartStatus()
    this.apply(d)
    d
  }

  //val log = org.slf4j.LoggerFactory.getLogger("Chart")
  private[this] var first = true
  def addAndApply(otherData:C, resetInit:Boolean=false) = {
    if (resetInit && first) {
      first = false
      applyOn(otherData)
    } else {
      apply {
        currentC = toPoints.append(currentC, otherData)
        currentPoints = toPoints(currentC, currentMax)
        updateChartStatus()
        val d = currentPoints map mToSeq
        this.apply(d)
        d
      }
    }
  }

  override val singleCodec = jsStringAnyCodec
  override val singleToO = identity[Seq[(String, Any)]] _

  val extendedContent:Option[scala.xml.Elem] = None

  override val content = Some {
    val container = <div>
      <span class="chart-total-item-count">{totalRowCount.toHtml} entries total</span>
      <span class="chart-sampling-warning">{warnSamplingInUse.toHtml}</span>
      <div>
      </div>
    </div>
    extendedContent.map(c => container.copy(child = container.child ++ c)).getOrElse(container)
  }
}

trait Sequencifiable[C] { self: Chart[C] =>
  val fields: Option[(String, String)]
  val groupField: Option[String]

  val (f1, f2)  = self.fields.getOrElse((headers(0), headers(1)))

  def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
    val stripedData = t.data.toSeq.filter{ case (k, v) =>
                        (groupField.isDefined && groupField.get == k) || (self.fields.isEmpty || f1 == k || f2 == k)
                      }
    stripedData
  }
}

trait Charts extends Utils {
  def tabs[C:ToPoints:Sampler](originalData:C, pages:Seq[(String, Chart[C])]) = Tabs(originalData, pages)

  def pairs[C:ToPoints:Sampler](originalData:C, maxPoints:Int=DEFAULT_MAX_POINTS) = {
    val data:Seq[MagicRenderPoint] = implicitly[ToPoints[C]].apply(originalData, maxPoints)
    val firstElem = data.head
    val headers = firstElem.headers
    lazy val dataMap = firstElem.data

    val ds = for {
      r <- headers
      c <- headers
    } yield {
      val (f1, f2)  = (dataMap(r), dataMap(c))
      if (isNumber(f1) && isNumber(f2)) {
        ScatterChart(originalData, Some((r, c)), (600/headers.size, 400/headers.size),maxPoints=maxPoints)
      } else if (isNumber(f2)) {
        BarChart(originalData, Some((r, c)), (600/headers.size, 400/headers.size),maxPoints=maxPoints)
      } else {
        TableChart(originalData, Some(List(r, c)), (600/headers.size, 400/headers.size),maxPoints=5)
      }
    }

    val m = ds grouped headers.size

    <table class="table" style="width: auto">
    <thead>{
      <tr>{headers.map{ h =>
        <th>{h}</th>
      }}</tr>
    }</thead>
    <tbody>{
      m.map { row =>
        <tr>{
          row.map { cell =>
            <td>{cell}</td>
          }
        }</tr>
      }
    }</tbody></table>
  }

  def display[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, maxPoints:Int=DEFAULT_MAX_POINTS):Widget = {
    val dataConverter = implicitly[ToPoints[C]]
    val initialDataToDisplay: Seq[MagicRenderPoint] = dataConverter(originalData, maxPoints)

    var allTabs: Seq[(String, Chart[C])] = Seq()

    allTabs :+= "table" → new TableChart(originalData, maxPoints=maxPoints) {
      override lazy val initialItems = initialDataToDisplay
    }

    // two field charts used only if dataset is non empty
    initialDataToDisplay.headOption match {
      case None => Nil
      case Some(firstElem) =>
        val members = firstElem.values
        val dataMap = firstElem.data
        val numOfFields = firstElem.numOfFields

        if (numOfFields == 2 || fields.isDefined) {
          val (f1, f2) = fields.map { case (f1, f2) => (dataMap(f1), dataMap(f2)) }
            .getOrElse((members(0), members(1)))

          if (isNumber(f1) && isNumber(f2)) {
            allTabs ++= Seq(
              "dot-circle-o" → new ScatterChart(originalData, fields, maxPoints = maxPoints) {
                override lazy val initialItems = initialDataToDisplay
              },
              "line-chart" → new LineChart(originalData, fields, maxPoints = maxPoints) {
                override lazy val initialItems = initialDataToDisplay
              })
          }
          if (isNumber(f2)) {
            allTabs :+= "bar-chart" → new BarChart(originalData, fields, maxPoints = maxPoints) {
              override lazy val initialItems = initialDataToDisplay
            }
          }

          if (!isNumber(f1)) {
            allTabs :+= "pie-chart" → new PieChart(originalData, fields, maxPoints = maxPoints) {
              override lazy val initialItems = initialDataToDisplay
            }
          }
        }
    }

    allTabs :+= "cubes" → new PivotChart(originalData, maxPoints=maxPoints) {
      override lazy val initialItems = initialDataToDisplay
    }
    tabs(originalData, allTabs)
  }
}
