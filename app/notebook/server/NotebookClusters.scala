package notebook.server

import java.io.{File, FileWriter}

import akka.actor._
import play.api.libs.json._
import play.api.{Configuration, Logger}

class NotebookClusters(store: File, initProfiles: Map[String, JsObject],
  initClusters: Map[String, JsObject]) extends Actor with ActorLogging {

  import NotebookClusters._

  var profiles: Map[String, JsObject] = initProfiles
  var clusters: Map[String, JsObject] = initClusters

  def receive = {
    case Add(name, o) =>
      clusters = clusters + (name → o)
      dump()
      sender ! o

    case Remove(name, o) =>
      clusters = clusters - name
      dump()

    case Get(name) =>
      sender ! clusters.get(name)

    case All =>
      sender ! clusters.values.toList

    case Profiles =>
      sender ! profiles.values.toList
  }

  def dump() {
    val j = Json.prettyPrint(JsObject(clusters.toSeq))
    val w = new FileWriter(store, false)
    try {
      w.write(j)
    } finally {
      w.close()
    }
  }
}

object NotebookClusters {

  case class Add(name: String, o: JsObject)

  case class Remove(name: String, o: JsObject)

  case class Get(name: String)

  case object All

  case object Profiles

  def apply(config: Configuration): NotebookClusters = {

    config.getString("profiles").foreach { p =>
      Logger.debug(s"Profiles file in the config is referring $p. Does it exist? ${new File(p).exists}")
    }
    val profilesFile = config.getString("profiles").map(new File(_)).filter(_.exists)
      .orElse(Option(new File("./conf/profiles"))).filter(_.exists) // ./bin/spark-notebook
      .getOrElse(new File("../conf/profiles")) // ./spark-notebook
    Logger.debug("Profiles file is : " + profilesFile)

    config.getString("file").foreach { p =>
      Logger.debug(s"Clusters file in the config is referring $p. Does it exist? ${new File(p).exists}")
    }
    val clustersFile = config.getString("file").map(new File(_)).filter(_.exists)
      .orElse(Option(new File("./conf/clusters"))).filter(_.exists) // ./bin/spark-notebook
      .getOrElse(new File("../conf/clusters")) // ./spark-notebook
    Logger.debug("Clusters file is : " + clustersFile)

    def readFileAsJsonConf(file: File) = {
      val source = scala.io.Source.fromFile(file)
      val lines = source.mkString
      source.close()

      val j = Json.parse(lines)
      val map = j match {
        case JsArray(xs) =>
          val v = xs.map(x => ((x \ "name").as[String], x)).toMap
          val m = v.collect { case x@(_, o: JsObject) => x }.toMap.asInstanceOf[Map[String, JsObject]]
          if (m.size != v.size) {
            Logger.warn("Some items have been discarded from clusters → no Json Objects!")
          }
          m

        case o@JsObject(xs) =>
          val v = o.value
          val m = v.collect { case x@(_, o: JsObject) => x }.toMap.asInstanceOf[Map[String, JsObject]]
          if (m.size != v.size) {
            Logger.warn("Some items have been discarded from clusters → no Json Objects!")
          }
          m
        case x => throw new IllegalStateException("Cannot load clusters got: " + x)
      }
      map
    }
    val initProfiles = readFileAsJsonConf(profilesFile)
    val initClusters = readFileAsJsonConf(clustersFile)
    new NotebookClusters(clustersFile, initProfiles, initClusters)
  }
}