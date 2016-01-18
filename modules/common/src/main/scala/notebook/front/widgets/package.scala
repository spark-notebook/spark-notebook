package notebook.front

import scala.util.Random
import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import com.vividsolutions.jts.geom.Geometry
import org.wololo.geojson.GeoJSON
import notebook._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._
import widgets.Defaults.DEFAULT_MAX_POINTS

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets {
  object Defaults {
    val DEFAULT_MAX_POINTS = 1000
  }
  def scopedScript(content: String, data: JsValue = null, selector:Option[String]=None) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    val withData = if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", Json.stringify(data), Null)

    val withSelector = selector.map { s =>
      withData % new UnprefixedAttribute("data-selector", s, Null)
    }.getOrElse(withData)
    withSelector
  }

  def text(value: String) = html(xml.Text(value))

  def text(value: Connection[String], style: Connection[String] = Connection.just("")) = {
    val _currentValue = JSBus.createConnection
    val stringCodec:Codec[JsValue, String] = formatToCodec(None)(implicitly[Format[String]])
    val currentValue = _currentValue biMap stringCodec
    currentValue <-- value

    val _currentStyle = JSBus.createConnection
    val currentStyle = _currentStyle biMap stringCodec
    currentStyle <-- style

    html(<p data-bind="text: value, style: style">{
      scopedScript(
        """
          |req(
          |['observable', 'knockout'],
          |function (O, ko) {
          |  ko.applyBindings({
          |      value: O.makeObservable(valueId),
          |      style: O.makeObservable(styleId)
          |    },
          |    this
          |  );
          |});
        """.stripMargin,
        Json.obj("valueId" -> _currentValue.id, "styleId" -> _currentStyle.id)
      )}</p>)
  }

  def ul(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None) = new DataConnectedWidget[String] {
    implicit val singleCodec:Codec[JsValue, String] = JsonCodec.strings

    var data = (initData.size, prefill) match {
      case (0, None) => Seq.empty[String]
      case (x, None) => initData
      case (0, Some(i)) => Seq.fill(capacity)(i)
      case (x, Some(i)) => initData.padTo(capacity, i)
    }

    apply(data)

    lazy val toHtml = <ul data-bind="foreach: value">
      <li data-bind="html: $data"></li>{
        scopedScript(
          """
              |req(
              |['observable', 'knockout'],
              |function (O, ko) {
              |  ko.applyBindings({
              |      value: O.makeObservable(valueId)
              |    },
              |    this
              |  );
              |});
          """stripMargin,
          Json.obj("valueId" -> dataConnection.id)
        )
      }</ul>

    override def apply(d:Seq[String]) {
      data = if (d.size > capacity) {
         d.drop(d.size - capacity)
      } else {
        d
      }
      super.apply(data)
    }

    def append(s:String) {
      apply(data :+ s)
    }

    def appendAll(s:Seq[String]) {
      apply(data ++ s)
    }
  }

  def out = new SingleConnectedWidget[String] {
    implicit val codec:Codec[JsValue, String] = formatToCodec(None)(Format.of[String])

    lazy val toHtml = <p data-bind="text: value">{
      scopedScript(
        """
            |req(
            |['observable', 'knockout'],
            |function (O, ko) {
            |  ko.applyBindings({
            |      value: O.makeObservable(valueId)
            |    },
            |    this
            |  );
            |});
        """.stripMargin,
        Json.obj("valueId" -> dataConnection.id)
      )}</p>
  }

  import java.awt.image.BufferedImage
  import java.io.ByteArrayOutputStream
  import javax.imageio.ImageIO

  def imageCodec(tpe:String) = new Codec[JsValue, BufferedImage] {
    def toBytes(bi:BufferedImage):String = {
      val bos = new ByteArrayOutputStream()
      ImageIO.write(bi, tpe, bos)
      val imageBytes = bos.toByteArray()
      val encodedImage = org.apache.commons.codec.binary.Base64.encodeBase64String(imageBytes)
      val imageString = "data:image/"+tpe+";base64,"+encodedImage
      bos.close()
      imageString
    }
    def decode(a: BufferedImage):JsValue = JsString(toBytes(a))
    def encode(v: JsValue):BufferedImage = ??? //todo
  }

  def img(tpe:String="png", width:String="150px", height:String="150px") = new SingleConnectedWidget[BufferedImage] {
    implicit val codec:Codec[JsValue, BufferedImage] = imageCodec(tpe)

    lazy val toHtml = <p>
      <img width={width} height={height} data-bind="attr:{src: value}" />
        {
          scopedScript(
            """
                |req(
                |['observable', 'knockout'],
                |function (O, ko) {
                |  ko.applyBindings({
                |      value: O.makeObservable(valueId)
                |    },
                |    this
                |  );
                |});
            """.stripMargin,
            Json.obj("valueId" -> dataConnection.id)
          )
        }
      </p>

      def url(u:java.net.URL) = apply(ImageIO.read(u))
      def file(f:java.io.File) = apply(ImageIO.read(f))

  }


  def html(html: NodeSeq): Widget = new SimpleWidget(html)

  def layout(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil): Widget = html(table(width, contents, headers ))

  def table(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil):Widget =
    <div class="table-container table-responsive">
    <table class="table">
      <thead>{
        (headers) grouped width map { row =>
          <tr>{
            row map { html => <th>{html}</th> }
          }</tr>
        }
    }
      </thead>
      <tbody>{
          (contents) grouped width map { row =>
          <tr>{
            row map { html => <td>{html}</td> }
          }</tr>
        }
      }
      </tbody>
    </table></div>

  def row(contents: Widget*)    = layout(contents.length, contents)
  def column(contents: Widget*) = layout(1, contents)

  def multi(widgets: Widget*) = html(NodeSeq.fromSeq(widgets.map(_.toHtml).flatten))

  def containerFluid(conf:List[List[(Widget,Int)]]):Widget = html(
    <div class="container-fluid">{
      conf.map { rows =>
        <div class="row">{
          rows.map { case (w, i) =>
            val cl = "col-md-"+i
            <div class={cl}>{w}</div>
          }
        }</div>
      }
    }</div>
  )

  import notebook.util.Reflector
  import java.util.Date

  implicit val jsStringAnyCodec:Codec[JsValue, Seq[(String, Any)]] = new Codec[JsValue, Seq[(String, Any)]] {
    def decode(a: Seq[(String, Any)]):JsValue = JsObject(a.map( f => f._1.trim -> toJson(f._2) ))
    def encode(v: JsValue):Seq[(String, Any)] = ??? //todo
  }

  def toJson(obj: Any): JsValue = {
    obj match {
      case null => JsNull
      case v: Int => JsNumber(v)
      case v: Float => JsNumber(v)
      case v: Double => JsNumber(v)
      case v: Long => JsNumber(v)
      case v: BigDecimal => JsNumber(v)
      case v: String => JsString(v)
      case v: Boolean => JsBoolean(v)
      case v: Geometry =>
        val json  = GeoChart.geometryToGeoJSON(v)
        val jsonstring = json.toString()
        Json.parse(jsonstring)
      case v: GeoJSON =>
        Json.parse(v.toString())
      case v: Iterable[_] =>
        val it = v.map(x => toJson(x))
        JsArray(it.toSeq)
      case v:Tuple2[_,_] =>
        JsObject(Seq(("_1" → toJson(v._1)), ("_2" → toJson(v._2))))
      case v:Tuple3[_,_,_] =>
        JsObject(Seq(("_1" → toJson(v._1)), ("_2" → toJson(v._2)), ("_3" → toJson(v._3))))
      case v =>
        JsString(v.toString)
    }
  }

  case class Tabs[C:ToPoints:Sampler](originalData:C, pages:Seq[(String, Chart[C])]=Nil, maxPoints:Int = DEFAULT_MAX_POINTS) extends JsWorld[Seq[(String, Any)], Seq[(String, Any)]] {
    import notebook.JSBus._

    def computePoints(max:Int = maxPoints) = implicitly[ToPoints[C]].apply(originalData, max)(implicitly[Sampler[C]])

    lazy val points:Seq[MagicRenderPoint] = computePoints()

    implicit val singleToO = identity[Seq[(String, Any)]] _

    implicit val singleCodec = jsStringAnyCodec

    def computeData(pts:Seq[MagicRenderPoint] = points) = pts.map(_.data.toSeq)
    override val data:Seq[Seq[(String, Any)]] = computeData()

    val maxPointsBox = new InputBox[Int](maxPoints, "Max Points (controlling all tabs)")

    maxPointsBox.currentData --> Connection.fromObserver { max:Int =>
      pages foreach { case (s, w) =>
        // following doesn't work because DOM don't get prg change event
        //   w.maxPointsBox.currentData <-- Connection.just(max)
        w.newMax(max)
      }
    }

    override val scripts = List(
      Script("magic/tabs", Json.obj())
    )

    override def apply(newData: Seq[Seq[(String,Any)]]) {
      super.apply(newData)
      pages.foreach { case (s, w) =>
        w(newData)
      }
    }

    override val content = Some {
      <div>
        {
          maxPointsBox.toHtml
        }
        <div >
          <ul class="nav nav-tabs" id={ "ul"+id }>{
            pages.zipWithIndex map { p: ((String, Widget), Int) =>
              <li>
                <a href={ "#tab"+id+"-"+p._2 }><i class={ "fa fa-"+p._1._1} /></a>
              </li>
            }
          }</ul>

          <div class="tab-content" id={ "tab"+id }>{
            pages.zipWithIndex map { p: ((String, Widget), Int) =>
              <div class="tab-pane" id={ "tab"+id+"-"+p._2 }>
              { p._1._2 }
              </div>
            }
          }</div>
        </div>
      </div>
    }
  }

  abstract class Chart[C:ToPoints:Sampler] extends JsWorld[Seq[(String, Any)], Seq[(String, Any)]] {
    import notebook.JSBus._

    def originalData:C
    def maxPoints:Int

    def sampler = implicitly[Sampler[C]]

    def toPoints = implicitly[ToPoints[C]]
    lazy val points:Seq[MagicRenderPoint] = toPoints(originalData, maxPoints)

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)]
    def computeData(pts:Seq[MagicRenderPoint] = points) = pts.map(mToSeq)
    lazy val data:Seq[Seq[(String, Any)]] = computeData(points)

    val maxPointsBox = new InputBox[Int](maxPoints, "Max Points")
    def sizes:(Int, Int)=(600, 400)

    lazy val nrow = out
    lazy val warnMax = out

    @volatile var currentC = originalData
    @volatile var currentPoints = points
    @volatile var currentMax = maxPoints

    def samplingWarning(maxEntriesLimit: Int): String = {
      if (currentMax >= toPoints.count(currentC)) {
        ""
      } else {
        sampler.samplingStrategy match {
          case magic.LimitBasedSampling() => " (Warning: showing first "+currentMax + " rows)"
          case _ => " (Warning: randomly sampled "+currentMax + " entries)"
        }
      }
    }

    maxPointsBox.currentData --> Connection.fromObserver { max:Int =>
      currentMax = max
      warnMax(samplingWarning(currentMax))
      applyOn(currentC)
    }

    def newMax(max:Int) = {
      //update javascript box
      maxPointsBox.currentData <-- Connection.just(max)
      //update state
      currentMax = max
      warnMax(samplingWarning(currentMax))
      applyOn(currentC)
    }

    def applyOn(newData:C) = apply {
      currentC = newData
      currentPoints = toPoints(newData, currentMax)
      val d = currentPoints map mToSeq
      nrow(toPoints.count(currentC)+ " entries")
      this.apply(d)
      d
    }

    //val log = org.slf4j.LoggerFactory.getLogger("Chart")
    def addAndApply(otherData:C) = apply {
      currentC = toPoints.append(currentC, otherData)
      currentPoints = toPoints(currentC, currentMax)
      nrow(toPoints.count(currentC)+ " entries")
      val d =  currentPoints map mToSeq
      this.apply(d)
      d
    }

    override val singleCodec = jsStringAnyCodec
    override val singleToO = identity[Seq[(String, Any)]] _

    lazy val firstElem = points.head
    lazy val headers = firstElem.headers
    lazy val members = firstElem.values
    lazy val dataMap = firstElem.data
    lazy val numOfFields = firstElem.numOfFields

    val extendedContent:Option[scala.xml.Elem] = None

    override val content = Some {
      val container = <div>
        {
          maxPointsBox.toHtml
        }
        {nrow.toHtml} <span style="color:red">{warnMax.toHtml}</span>
        <div>
        </div>
      </div>
      extendedContent.map(c => container.copy(child = container.child ++ c)).getOrElse(container)
    }
  }

  case class PivotChart[C:ToPoints:Sampler](
    originalData:C,
    override val sizes:(Int, Int)=(600, 400),
    maxPoints:Int = DEFAULT_MAX_POINTS,
    derivedAttributes:JsObject=play.api.libs.json.Json.obj(),
    options: Map[String, String] = Map.empty
  ) extends Chart[C] {
    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

    protected def optionsJson = Json.obj(options.mapValues(Json.toJsFieldJsValueWrapper(_)).toSeq: _*)

    override val scripts = List(Script( "magic/pivotChart", Json.obj("width" → sizes._1,
                                                                     "height" → sizes._2,
                                                                     "derivedAttributes" → derivedAttributes,
                                                                     "extraOptions" → optionsJson)
    ))
  }

  case class ScatterChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    val (f1, f2)  = fields.getOrElse((headers(0), headers(1)))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter{case (k, v) => !fields.isDefined || f1 == k || f2 == k }
      stripedData
    }

    override val scripts = List(Script( "magic/scatterChart",
                                        Json.obj( "x" → f1.toString, "y" → f2.toString,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

  case class LineChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    val (f1, f2)  = fields.getOrElse((headers(0), headers(1)))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter{case (k, v) => !fields.isDefined || f1 == k || f2 == k }
      stripedData
    }


    override val scripts = List(Script( "magic/lineChart",
                                        Json.obj( "x" → f1.toString, "y" → f2.toString,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

  case class BarChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    val (f1, f2)  = fields.getOrElse((headers(0), headers(1)))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter{case (k, v) => !fields.isDefined || f1 == k || f2 == k }
      stripedData
    }

    override val scripts = List(Script( "magic/barChart",
                                        Json.obj( "x" → f1.toString, "y" → f2.toString,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

  case class PieChart[C:ToPoints:Sampler](originalData:C, fields:Option[(String, String)]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    val (f1, f2)  = fields.getOrElse((headers(0), headers(1)))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter{case (k, v) => !fields.isDefined || f1 == k || f2 == k }
      stripedData
    }

    override val scripts = List(Script( "magic/pieChart",
                                        Json.obj("series" → f1.toString, "p" → f2.toString,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

  case class GeoPointsChart[C:ToPoints:Sampler](
    originalData:C,
    override val sizes:(Int, Int)=(600, 400),
    maxPoints:Int = DEFAULT_MAX_POINTS,
    latLonFields:Option[(String, String)]=None,
    rField:Option[String]=None,
    colorField:Option[String]=None) extends Chart[C] {

    //val (f1, f2)  = fields.getOrElse((headers(0), headers(1)))

    val latLong = latLonFields.getOrElse((headers(0), headers(1)))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter { case (k, v) =>
                                              k == latLong._1 ||
                                              k == latLong._2 ||
                                              Some(k) == rField ||
                                              Some(k) == colorField
                                            }
      stripedData
    }

    override val scripts =
      List(Script("magic/geoPointsChart",
        Json.obj(
                  "lat" → latLong._1, "lon" → latLong._2,
                  "width" → sizes._1, "height" → sizes._2,
                  "rField" → rField, "colorField" → colorField
                  /*, "proj" → proj, "baseMap" → baseMap*/
                )
        ++ rField.map(r => Json.obj("r" → r)).getOrElse(Json.obj())
        ++ colorField.map(color => Json.obj("color" → color)).getOrElse(Json.obj())
      ))
  }

  case class GeoChart[C:ToPoints:Sampler](
    originalData:C,
    override val sizes:(Int, Int)=(600, 400),
    maxPoints:Int = DEFAULT_MAX_POINTS,
    geometryField:Option[String]=None,
    rField:Option[String]=None,
    colorField:Option[String]=None,
    fillColorField:Option[String]=None) extends Chart[C] {

    val geometry = geometryField.getOrElse(headers(0))

    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      val stripedData = t.data.toSeq.filter { case (k, v) =>
                                              k == geometry ||
                                              Some(k) == rField ||
                                              Some(k) == colorField  ||
                                              Some(k) == fillColorField
                                            }
      stripedData
    }

    override val scripts =
      List(Script("magic/geoChart",
        Json.obj(
          "geometry" → geometry,
          "width" → sizes._1, "height" → sizes._2
        )
        ++ rField.map(r => Json.obj("r" → r)).getOrElse(Json.obj())
        ++ colorField.map(color => Json.obj("color" → color)).getOrElse(Json.obj())
        ++ fillColorField.map(color => Json.obj("fillColor" → color)).getOrElse(Json.obj())
      ))
  }

  object GeoChart {
    def parseGeoJSON(s:String):GeoJSON = org.wololo.geojson.GeoJSONFactory.create(s)
    def geometryToGeoJSON(g:Geometry):GeoJSON = {
      import org.wololo.jts2geojson.GeoJSONWriter
      val writer = new GeoJSONWriter()
      val json:GeoJSON = writer.write(g)
      json
    }
  }

  case class GraphChart[C:ToPoints:Sampler](originalData:C, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS, charge:Int= -30, linkDistance:Int=20, linkStrength:Double=1.0) extends Chart[C] {
    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq


    val opts = Json.obj("headers" → headers, "width" → sizes._1,
                        "height" → sizes._2, "charge" → charge, "linkDistance" → linkDistance,
                        "linkStrength" → linkStrength)

    override val scripts = List(Script("magic/graphChart", opts))
  }

  case class DiyChart[C:ToPoints:Sampler](originalData:C, js:String = "function(data, headers, chart) { console.log({'data': data, 'headers': headers, 'chart': chart}); }", override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

    override val scripts = List(Script( "magic/diyChart",
                                        Json.obj( "js" → s"var js = $js;", "headers" → headers,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

  case class CustomC3Chart[C:ToPoints:Sampler](originalData:C, chartOptions :String = "{}", override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = t.data.toSeq

    override val scripts = List(Script( "magic/customC3Chart",
      Json.obj(
        "js" → s"var chartOptions = $chartOptions;",
        "headers" → headers,
        "height" → sizes._2)))
  }

  case class TableChart[C:ToPoints:Sampler](originalData:C, filterCol:Option[Seq[String]]=None, override val sizes:(Int, Int)=(600, 400), maxPoints:Int = DEFAULT_MAX_POINTS) extends Chart[C] {
    def mToSeq(t:MagicRenderPoint):Seq[(String, Any)] = {
      t.data.toSeq.filter{case (k, v) => filterCol.getOrElse(headers).contains(k)}
    }
    val h:Seq[String] = filterCol.getOrElse(headers)
    override val scripts = List(Script( "magic/tableChart",
                                        Json.obj( "headers" → h,
                                                  "width" → sizes._1, "height" → sizes._2)))
  }

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

    <table class="table" style="width: 600px">
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
    val data:Seq[MagicRenderPoint] = implicitly[ToPoints[C]].apply(originalData, maxPoints)
    val firstElem = data.head
    val headers = firstElem.headers
    val members = firstElem.values
    val dataMap = firstElem.data

    val numOfFields = firstElem.numOfFields

    val tbl = Some("table" → TableChart(originalData, maxPoints=maxPoints))

    val twoFieldCharts = if(numOfFields == 2 || fields.isDefined){
      val (f1, f2)  = fields.map{ case (f1, f2) => (dataMap(f1), dataMap(f2)) }
                            .getOrElse((members(0), members(1)))

      val scatter:Option[(String, Chart[C])] = if (isNumber(f1) && isNumber(f2)) { Some("dot-circle-o" → ScatterChart(originalData, fields,maxPoints=maxPoints)) } else None
      val line:Option[(String, Chart[C])]    = if (isNumber(f1) && isNumber(f2)) { Some("line-chart" → LineChart(originalData, fields,maxPoints=maxPoints)) } else None
      val bar :Option[(String, Chart[C])]    = if (isNumber(f2)) { Some("bar-chart" → BarChart(originalData, fields,maxPoints=maxPoints)) } else None
      val pie :Option[(String, Chart[C])]    = if (!isNumber(f1)) { Some("pie-chart" → PieChart(originalData, fields,maxPoints=maxPoints)) } else None

      scatter :: line :: bar :: pie :: Nil
    } else Nil

    val pivot = Some("cubes" → PivotChart(originalData, maxPoints=maxPoints))

    val allTabs = (tbl :: twoFieldCharts ::: pivot :: Nil).collect { case Some(t) => t }
    tabs(originalData, allTabs)
  }

  def isNumber(obj: Any) = obj.isInstanceOf[Int] || obj.isInstanceOf[Float] || obj.isInstanceOf[Double] || obj.isInstanceOf[Long]
  def isDate(obj: Any) = obj.isInstanceOf[Date]
}