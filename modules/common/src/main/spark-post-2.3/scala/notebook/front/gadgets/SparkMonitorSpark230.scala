package org.apache.spark.ui.notebook.front.gadgets

import notebook.JobTracking

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import org.apache.spark.SparkContext
import org.apache.spark.scheduler.StageInfo

import play.api.libs.json._

import notebook.util.Logging

case class JobGroupInfo(jobGroup: String,
                        completedTasks: Int,
                        totalTasks: Int)

// Dummy at the moment to make things work. no actual progress is reported
class SparkMonitor(sparkContext:SparkContext, checkInterval:Long = 1000) extends Logging {
  def sparkUiLink: Option[String] = sparkContext.master match {
    case m if m.startsWith("yarn") =>
      sys.env.get("YARN_JOB_PROXY_URL")
        .map(yarnProxyURL => s"${yarnProxyURL}/${sparkContext.applicationId}")
    case _ =>
      sparkContext.ui.map(sparkUI => sparkUI.webUrl)
  }

  val connection = notebook.JSBus.createConnection("jobsProgress")

  private[this] var t:Option[Thread] = None

  def getJobGroupProgress(jobGroup: String) = {
    val sc = sparkContext
    // based on https://github.com/apache/zeppelin/pull/2750/files#diff-58542af0ab98166a4e47ff0af919cbd5R35
    val jobIds = sc.statusTracker.getJobIdsForGroup(jobGroup)
    val jobs = jobIds.flatMap { id => sc.statusTracker.getJobInfo(id) }
    val stages = jobs.flatMap { job =>
      job.stageIds().flatMap(sc.statusTracker.getStageInfo)
    }

    val taskCount = stages.map(_.numTasks).sum
    val completedTaskCount = stages.map(_.numCompletedTasks).sum

    JobGroupInfo(jobGroup = jobGroup, completedTasks = completedTaskCount, totalTasks = taskCount)
  }

  def fetchAllJobStatus: Seq[JsObject] = {
    val allJobGroups = sparkContext.statusStore.jobsList(statuses = null).flatMap(_.jobGroup)
    allJobGroups.map(getJobGroupProgress).map { j =>
      val jobGroup = j.jobGroup
      val cellId = JobTracking.toCellId(Some(jobGroup))
      val jobDuration: Option[Long] = None
      val jobDurationStr = "N/A"

      Json.obj(
        //"id" → j.jobId,
        //"job" → j.jobId,
        "group" → jobGroup,
        "cell_id" → cellId,
        "name" → jobGroup,
        "completed" → (j.completedTasks.toDouble / j.totalTasks * 100),
        "completed_tasks" -> j.completedTasks,
        "total_tasks" -> j.totalTasks,
        "duration_millis" → jobDuration,
        "time" → jobDurationStr
      )
    }
  }

  private[this] def newT ={
    new Thread(){
      override def run =
        while(true) {
          Thread.sleep(2000)
          val m = fetchAllJobStatus
          connection <-- notebook.Connection.just(
            JsObject(Seq(
              "jobsStatus" -> JsArray(m),
              "sparkUi" -> JsString(sparkUiLink.getOrElse(""))
            )))
        }
    }
  }

  def start() = {
    t.foreach(_.stop)
    t = Some(newT)
    t.foreach(_.start)
  }

  def stop() = {
    t.foreach(_.stop)
    t = None
  }

}
