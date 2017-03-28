package notebook.io

import java.nio.file.Path

import notebook.Notebook

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class TestBase extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  val DefaultWait = 5 //seconds
  val DefaultWaitSeconds = 5 seconds
  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

  var provider: NotebookProvider = _
  var gitNotebookProvider: ConfigurableGitNotebookProvider = _

  var emptyTemp: Path = _
  var nonEmptyTemp: Path = _
  var target: Path = _

  def setProvider(fProv: Future[NotebookProvider]): Unit = {
    provider = Await.result(fProv, DefaultWaitSeconds)
    gitNotebookProvider = provider match {
      case x: ConfigurableGitNotebookProvider => x
      case _ => throw new RuntimeException("Invalid provided type detected at runtime")
    }
  }
}

object TestData  {

  val notebookSer =
    """{
      |  "metadata" : {
      |    "id" : "foo-bar-id",
      |    "name" : "test-notebook-name",
      |    "user_save_timestamp" : "1999-09-09T09:09:09.000Z",
      |    "auto_save_timestamp" : "2001-01-01T00:00:00.000Z",
      |    "language_info" : {
      |      "name" : "scala",
      |      "file_extension" : "scala",
      |      "codemirror_mode" : "text/x-scala"
      |    },
      |    "trusted" : true,
      |    "sparkNotebook" : {
      |      "build" : "unit-tests"
      |    },
      |    "customLocalRepo" : "local-repo",
      |    "customRepos" : [ "custom-repo" ],
      |    "customDeps" : [ "\"org.custom\" % \"dependency\" % \"1.0.0\"" ],
      |    "customImports" : [ "import org.cusom.dependency.SomeClass" ],
      |    "customArgs" : [ ],
      |    "customSparkConf" : {
      |      "spark.driverPort" : 1234
      |    }
      |  },
      |  "cells" : [ ]
      |}
    """.stripMargin

  def notebook(): Notebook  = {
    // weird sequence to normalize serialization artifacts (JSON indentation)
    val deserNotebook = Notebook
      .deserializeFuture(notebookSer)
      .flatMap { nb =>
        Notebook.serializeFuture(nb).flatMap(snb => Notebook.deserializeFuture(snb))
      }
    Await.result(deserNotebook, 5 second)
  }

  val testName = "test-notebook-name"
  val sparkNotebook = Map("build" -> "unit-tests")
  val customLocalRepo = Some("local-repo")
  val customRepos = Some(List("custom-repo"))
  val customDeps = Some(List(""""org.custom" % "dependency" % "1.0.0""""))
  val customImports = Some(List("""import org.cusom.dependency.SomeClass"""))
  val customArgs = Some(List.empty[String])
  val customSparkConf = Some(JsObject( List(("spark.driverPort", JsNumber(1234))) ))

}