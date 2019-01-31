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

case class JobInfo(jobId: Int,
                   completedTasks: Int,
                   totalTasks: Int,
                   submissionTime: Option[Long],
                   completionTime: Option[Long])

class SparkMonitor(sparkContext:SparkContext, checkInterval:Long = 1000) extends Logging {

  val connection = notebook.JSBus.createConnection("jobsProgress")

  val listener = new org.apache.spark.ui.jobs.JobProgressListener(sparkContext.getConf)

  sparkContext.listenerBus.addListener(listener)

  /**
    * Identify link to Spark UI. Supports `yarn-*` modes so far.
    */
  def sparkUiLink: Option[String] = sparkContext.master match {
    case m if m.startsWith("yarn") =>
      sys.env.get("YARN_JOB_PROXY_URL")
        .map(yarnProxyURL => s"${yarnProxyURL}/${sparkContext.applicationId}")
    case _ =>
      try {
        val u = sparkContext.getClass.getMethod("ui")
        u.setAccessible(true)
        val ui = u.invoke(sparkContext).asInstanceOf[Option[Any]]
        val appUIAddress:Option[String] = ui.map{ u =>
          val a = u.getClass.getMethod("appUIAddress")
          a.setAccessible(true)
          val appUIAddress = a.invoke(u)
          appUIAddress.toString
        }
        appUIAddress
      } catch {
        // reflective methods above may throw: SecurityException | ReflectiveOperationException | IllegalArgumentException
        // these are all RuntimeException, so at least catch this instead of catching any Exception
        case e: RuntimeException =>
          logWarn("Unable to determine URL for sparkUI", e)
          None
      }
  }

  def minOption(s: Iterable[Long]) = if (s.isEmpty) None else Some(s.min)
  def maxOption(s: Iterable[Long]) = if (s.isEmpty) None else Some(s.max)

  def fetchMetrics = {
    listener.synchronized {
      val activeStages = listener.activeStages.values.toSeq
      val completedStages = listener.completedStages.reverse.toSeq
      val failedStages = listener.failedStages.reverse.toSeq
      val pendingStages = listener.pendingStages.values.toSeq
      val now = System.currentTimeMillis

      val activeStagesList = activeStages.sortBy(_.submissionTime).reverse
      val completedStagesList = completedStages.sortBy(_.submissionTime).reverse
      val failedStagesList = failedStages.sortBy(_.submissionTime).reverse

      val activeJobs = listener.activeJobs.values.toList
      val completedJobs = listener.completedJobs.toList
      val failedJobs = listener.failedJobs.toList

      val jobsByStageId = (for {
        j <- activeJobs ::: completedJobs ::: failedJobs
        stageId <- j.stageIds
      } yield stageId → j).toMap

      val jobGroupsById = jobsByStageId.values.groupBy(_.jobId).mapValues(_.head.jobGroup)

      val stageStats: Seq[JobInfo] = (activeStagesList ++ completedStages ++ pendingStages).flatMap { s: StageInfo =>
        val stageDataOption = listener.stageIdToData.get((s.stageId, s.attemptId))
        stageDataOption.map { stageData =>
          JobInfo(
            jobId = jobsByStageId(s.stageId).jobId,
            completedTasks = stageData.completedIndices.size,
            totalTasks = s.numTasks,
            submissionTime = s.submissionTime,
            completionTime = s.completionTime
          )
        }
      }

      val jobStats: Seq[JobInfo] = stageStats
        .groupBy(_.jobId)
        .map { case (jobId, jobStages) =>
          jobStages.reduce { (j1: JobInfo, j2: JobInfo) =>
            JobInfo(
              jobId = j1.jobId,
              completedTasks = j1.completedTasks + j2.completedTasks,
              totalTasks = j1.totalTasks + j2.totalTasks,
              submissionTime = minOption(j1.submissionTime ++ j2.submissionTime),
              completionTime = maxOption(j1.completionTime ++ j2.completionTime)
            )
          }
        }.toSeq

      jobStats.map { j =>
        val jobGroup = jobGroupsById(j.jobId)
        val cellId = JobTracking.toCellId(jobGroup)
        val jobDuration: Option[Long] = j.submissionTime.map(t => j.completionTime.getOrElse(System.currentTimeMillis) - t)
        val jobDurationStr = jobDuration.map { d =>
          s"${(d.toFloat / 1000).formatted("%.2f")}s"
        }.getOrElse("N/A")

        Json.obj(
          "id" → j.jobId,
          "job" → j.jobId,
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
  }

  private[this] var t:Option[Thread] = None

  private[this] def newT ={
    new Thread(){
      override def run =
        while(true) {
          Thread.sleep(2000)
          val m = fetchMetrics
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
