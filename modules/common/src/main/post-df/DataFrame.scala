package notebook.front.widgets

import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.runtime.universe.TypeTag

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, DataFrame}
import play.api.libs.json._

import notebook._
import notebook.front.{DataConnector, SingleConnector,Widget}
import notebook.front.widgets.Utils

/**
 * An abstract view of a dataframe.
 *
 * The view provides connectors to view the dataframe one partition at a time.   A widget
 * may inherit this trait to provide a specific rendering.
 */
trait DataFrameView {
  def data: DataFrame
  def pageSize:Int

  /* paging support */
  val partitionIndexConnector = new SingleConnector[Int]() {
    override implicit def codec: Codec[JsValue, Int] = JsonCodec.ints
  }

  partitionIndexConnector.currentData --> Connection.fromObserver(index => {
    if (index < pages) {
      select(index)
    }
  })

  /* data access */
  val dataConnector = new DataConnector[JsValue]() {
    override implicit def singleCodec: Codec[JsValue, JsValue] = JsonCodec.idCodec
  }

  private lazy val json: RDD[(String, Long)] = data.toJSON.zipWithIndex.cache
  val count = json.count
  val pages = (count / pageSize) + (if (count.toDouble / pageSize == count / pageSize) 0 else 1)

  private def select(partitionIndex: Int): Unit = {
    val scope = new java.io.Serializable {
      val pi = partitionIndex
      val ps = pageSize
      val skipper = (ki:(String, Long)) => ki._2 >= (pi*ps)
      val _1 = (ki:(String, Long)) => ki._1
      //import ExecutionContext.Implicits.global
      val job = Future.successful {
        json.filter(skipper)
            .take(ps)
            .map(_1)
            .map(Json.parse)
            .toSeq
      }
    }
    // connect to the job results (which are emitted asynchronously)
    dataConnector.currentData <-- Connection.fromObservable(Observable.from(scope.job))
  }
}

class DataFrameWidget(
  override val data: DataFrame,
  override val pageSize: Int = 25,
  extension: String
) extends Widget
  with DataFrameView
  with Utils {

  private val js = List("dataframe", extension).map(
    x => s"'../javascripts/notebook/$x'").mkString("[", ",", "]")
  private val call = {
    // data ==> data-this (in observable.js's scopedEval) ==> this in JS => { dataId, dataInit, ... }
    // this ==> scope (in observable.js's scopedEval) ==> this.parentElement ==> div.container below (toHtml)
    s"""
      function(dataframe, extension) {
        dataframe.call(data, this, extension);
      }
    """
  }

  lazy val toHtml =
    <div class="df-canvas">
      {scopedScript(
      s"req($js, $call);",
      Json.obj(
        "dataId" -> dataConnector.dataConnection.id,
        "partitionIndexId" -> partitionIndexConnector.dataConnection.id,
        "numPartitions" -> pages,
        "dfSchema" -> Json.parse(data.schema.json)
      )
      )}
      <link rel="stylesheet" href="/assets/stylesheets/ipython/css/dataframe.css" type="text/css" />
    </div>
}

object DataFrameWidget {

  def table(
    data: DataFrame,
    pageSize: Int
  ): DataFrameWidget = {
    new DataFrameWidget(data, pageSize, "consoleDir")
  }

  def table[A <: Product : TypeTag](
    rdd: RDD[A],
    pageSize: Int
  ): DataFrameWidget = {
    val sqlContext = new SQLContext(rdd.sparkContext)
    import sqlContext.implicits._
    table(rdd.toDF(), pageSize)
  }
}
