package notebook
package server

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._

import play.api.libs.json._

class NotebookClusters(initClusters: Map[String, JsObject] = NotebookClusters.examples) extends Actor with ActorLogging {
  import NotebookClusters._

  var clusters:Map[String, JsObject] = initClusters

  def receive = {
    case Add(name, o)    =>
      clusters = clusters + (name → o)
      sender ! o
    case Remove(name, o) => clusters = clusters - name
    case Get(name)       => sender ! clusters.get(name)
    case All             => sender ! clusters.values.toList
  }
}

object NotebookClusters {
  case class Add(name:String, o:JsObject)
  case class Remove(name:String, o:JsObject)
  case class Get(name:String)
  case object All

  val standalone = """
  |{
  |  "spark.app.name": "Notebook",
  |  "spark.master": "spark://<master>:<port>",
  |  "spark.executor.memory": "512m"
  |}
  """.stripMargin

  val mesos = """
  |{
  |  "spark.app.name": "Notebook",
  |  "spark.master": "mesos://<master>:<port>",
  |  "spark.executor.memory": "512m",
  |  "spark.executor.uri": "hdfs://<spark>.tgz",
  |  "spark.driver.host": "<host>",
  |  "spark.local.dir": "<path>"
  |}
  """.stripMargin

  val examples:Map[String, JsObject] = Map(
    ("Standalone" → Json.parse(
      s"""
      |{
      |  "profile": "Standalone",
      |  "name": "ec2 <host>",
      |  "status": "stopped",
      |  "template" : $standalone
      |}
      |""".stripMargin.trim
    ).asInstanceOf[JsObject]),
    ("Mesos" → Json.parse(
      s"""
      |{
      |  "profile": "Mesos",
      |  "name": "Integration env",
      |  "status": "stopped",
      |  "template" : $mesos
      |}
      |""".stripMargin.trim
    ).asInstanceOf[JsObject])
  )
}