package notebook.front.widgets

import notebook._
import notebook.front.{DataConnector, SingleConnector,Widget}
import org.apache.spark.FutureAction
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import play.api.libs.json._

/**
 * An abstract view of a dataframe.
 *
 * The view provides connectors to view the dataframe one partition at a time.   A widget
 * may inherit this trait to provide a specific rendering.
 */
trait DataFrameView {
  val data: DataFrame

  /* paging support */
  val partitionIndexConnector = new SingleConnector[Int]() {
    override implicit def codec: Codec[JsValue, Int] = JsonCodec.ints
  }

  partitionIndexConnector.currentData --> Connection.fromObserver(index => {
    if(partitions.indices contains index) {
      select(index)
    }
  })

  /* data access */
  val dataConnector = new DataConnector[JsValue]() {
    override implicit def singleCodec: Codec[JsValue, JsValue] = JsonCodec.idCodec
  }

  lazy val partitions: Array[org.apache.spark.Partition] = json.partitions

  private lazy val json: RDD[String] = data.toJSON

  private var currentJob: Option[FutureAction[Seq[JsValue]]] = None

  private def select(partitionIndex: Int): Unit = {

    if(!(partitions.indices contains partitionIndex))
      throw new IllegalArgumentException(s"index $partitionIndex out of range ${partitions.indices}")

    val sc = data.sqlContext.sparkContext

    synchronized {
      // cancel the active job if any
      currentJob match {
        case Some(job) => if(!job.isCompleted) job.cancel()
        case None =>
      }

      // schedule a Spark job to collect the given partition
      var result: Seq[JsValue] = null
      val job = sc.submitJob[String, Array[String], Seq[JsValue]](
        json, _.toArray, Seq(partitionIndex),
        (index, data) => result = data.map { row:String => Json.parse(row)},
        result)
      currentJob = Some(job)

      // connect to the job results (which are emitted asynchronously)
      dataConnector.currentData <-- Connection.fromObservable(Observable.from(job))
    }
  }
}

class DataFrameWidget(
  override val data: DataFrame,
  width: Int = 600,
  height: Int = 400,
  extension: String
)
  extends Widget with DataFrameView {

  private val js = List("dataframe", extension).map(
    x => s"'../javascripts/notebook/$x'").mkString("[", ",", "]")
  private val call = {
    s"""
      function(dataframe, extension) {
        // data ==> data-this (in observable.js's scopedEval) ==> this in JS => { dataId, dataInit, ... }
        // this ==> scope (in observable.js's scopedEval) ==> this.parentElement ==> div.container below (toHtml)

        dataframe.call(data, this, extension);
      }
    """
  }
  
  lazy val toHtml =
    <div>
      {scopedScript(
      s"req($js, $call);",
      Json.obj(
        "dataId" -> dataConnector.dataConnection.id,
        "partitionIndexId" -> partitionIndexConnector.dataConnection.id,
        "numPartitions" -> partitions.length,
        "dfSchema" -> Json.parse(data.schema.json)
      )
      )}
        <table class="dataframe" width={width.toString} height={height.toString}>
        </table>
        <form role="form" id="{htmlId}">
          <button class="btn btn-default" data-bind="click: prevPartition">Prev</button>
          <button class="btn btn-default" data-bind="click: nextPartition">Next</button>
        </form>
      </div>
}

object DataFrameWidget {

  def table(
    data: DataFrame,
    width: Int = 600,
    height: Int = 400
  ): DataFrameWidget = {
    new DataFrameWidget(data, width, height, "consoleDir")
  }
}
