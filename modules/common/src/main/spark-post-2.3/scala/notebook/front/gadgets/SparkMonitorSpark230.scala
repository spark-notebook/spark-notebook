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

// Dummy at the moment to make things work. no actual progress is reported
class SparkMonitor(sparkContext:SparkContext, checkInterval:Long = 1000) extends Logging {
  def sparkUiLink = None

  val connection = notebook.JSBus.createConnection("jobsProgress")

  private[this] var t:Option[Thread] = None

  private[this] def newT ={
    new Thread(){
      override def run =
        while(true) {
          Thread.sleep(2000)
          val m = Seq()
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
