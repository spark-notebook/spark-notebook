package org.apache.spark.ui.notebook.front.widgets

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._
import scala.concurrent.duration._

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import org.apache.spark.SparkContext
import org.apache.spark.scheduler.StageInfo


import notebook._, JSBus._
import notebook.front._
import notebook.front.widgets._

class SparkInfo(sparkContext:SparkContext, checkInterval:Duration=5 seconds, execNumber:Option[Int]=None) extends SingleConnector[JValue] with Widget {
  implicit val codec:Codec[JValue, JValue] = JsonCodec.idCodec[JValue]

  val listener = new org.apache.spark.ui.jobs.JobProgressListener(sparkContext.getConf)

  sparkContext.listenerBus.addListener(listener)

  def fetchMetrics:Future[JObject] = Future { listener.synchronized {
    val activeStages = listener.activeStages.values.toSeq
    val completedStages = listener.completedStages.reverse.toSeq
    val failedStages = listener.failedStages.reverse.toSeq
    val now = System.currentTimeMillis

    val activeStagesList = activeStages.sortBy(_.submissionTime).reverse
    val completedStagesList = completedStages.sortBy(_.submissionTime).reverse
    val failedStagesList = failedStages.sortBy(_.submissionTime).reverse

    val stageExtract = (s:StageInfo) => {
      val stageDataOption = listener.stageIdToData.get((s.stageId, s.attemptId))
      stageDataOption.map{ stageData =>
        val started = stageData.numActiveTasks
        val completed = stageData.completedIndices.size
        val failed = stageData.numFailedTasks
        val total = s.numTasks
        ("name" -> s.name) ~
        ("details" -> s.details) ~
        ("completed" -> completed) ~
        ("started" -> started) ~
        ("total" -> total) ~
        ("failed" -> failed) ~
        ("progress" -> s"${completed.toDouble / total * 100}")
      }
    }

    ("duration" -> (now-sparkContext.startTime)) ~
    ("mode" -> listener.schedulingMode.map(_.toString).getOrElse("Unknown")) ~
    ("activeNb" -> activeStages.size) ~
    ("completedNb" -> completedStages.size) ~
    ("failedNb" -> failedStages.size) ~
    ("activeStages" -> (activeStagesList map stageExtract)) ~
    ("completedStages" -> (completedStagesList map stageExtract))
  }}

  def exec(n:Int=Int.MaxValue):Unit = fetchMetrics.onComplete {
    case Success(x) =>
      apply(x)
      Thread.sleep(checkInterval.toMillis)
      if (n>1) exec(n-1) else ()
    case Failure(ex) =>
      apply("error" -> ("ERROR (stopping)>>>>"+ex.getMessage))
  }

  // start the checks
  execNumber.map(x => exec(x)).getOrElse(exec())

  lazy val toHtml =
      <div data-bind="with: value">{
      scopedScript(
        """ require(
              ['observable', 'knockout', 'knockout-bootstrap'],
              function (O, ko) {
                v_v_v = O.makeObservable(valueId);
                v_v_v.subscribe(function (x) {console.dir(x);});
                ko.applyBindings(
                  {
                    value: v_v_v
                  },
                  this
                );
              }
            );
        """,
        ("valueId" -> dataConnection.id)
      )}<ul class="unstyled">
          <li>
            <strong>Total Duration: </strong>
            <span data-bind="text: duration"></span>
          </li>
          <li>
            <strong>Scheduling Mode: </strong>
            <span data-bind="text: mode"></span>
          </li>
          <li>
            <strong>Active Stages:</strong>
            <span data-bind="text: activeNb"></span>
          </li>
          <li>
            <strong>Completed Stages:</strong>
            <span data-bind="text: completedNb"></span>
          </li>
          <li>
            <strong>Failed Stages:</strong>
            <span data-bind="text: failedNb"></span>
          </li>
        </ul>
        <hr/>
        <h4>Active Stages</h4>
        <div data-bind="foreach: activeStages">
          <h5 data-bind="attr: { title: details }"><span data-bind="text: name"></span></h5>
          <ul>
            <li>Total: <span data-bind="text: total" style="color: blue"></span></li>
            <li>Failed: <span data-bind="text: failed" style="color: red"></span></li>
            <li>Progress: <span data-bind="text: progress"></span>%</li>
          </ul>
          <div data-bind="progress: progress"></div>
        </div>
        <hr/>
        <h4>Completed Stages</h4>
        <div data-bind="foreach: completedStages">
          <h5 data-bind="attr: { title: details }"><span data-bind="text: name"></span></h5>
          <ul>
            <li>Total: <span data-bind="text: total" style="color: blue"></span></li>
            <li>Failed: <span data-bind="text: failed" style="color: red"></span></li>
          </ul>
        </div>
        <hr/>
      </div>


}