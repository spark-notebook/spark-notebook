package notebook.io


import java.nio.file.{Files, Path, Paths}

import com.typesafe.config.ConfigFactory
import notebook.NBSerializer.Metadata
import notebook.Notebook
import org.apache.commons.io.FileUtils
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject}
import play.libs.Json

import scala.collection.JavaConverters._
import scala.concurrent.{Await}
import scala.concurrent.duration._
import scala.io.Source
import scala.util.Try

class FileSystemNotebookProviderTests extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  import scala.concurrent.ExecutionContext.Implicits.global

  val DefaultWait = 2 //seconds
  val DefaultWaitSeconds = DefaultWait seconds
  implicit val defaultPatience =
    PatienceConfig(timeout =  Span(DefaultWait, Seconds), interval = Span(5, Millis))

  var tempPath: Path = _
  var provider: NotebookProvider = _
  var notebookPath: Path = _

  val id = "foo-bar-loo-lar"
  val testName = "test-notebook-name"
  val sparkNotebook = Map("build" -> "unit-tests")
  val customLocalRepo = Some("local-repo")
  val customRepos = Some(List("custom-repo"))
  val customDeps = Some(List(""""org.custom" % "dependency" % "1.0.0""""))
  val customImports = Some(List("""import org.cusom.dependency.SomeClass"""))
  val customArgs = Some(List.empty[String])
  val customSparkConf = Some(JsObject( List(("spark.driverPort", JsNumber(1234))) ))

  val metadata = new Metadata(
    id =  id,
    name = testName,
    user_save_timestamp =  new DateTime(1999, 9, 9, 9, 9, 9,DateTimeZone.forID("CET")).toDate,
    auto_save_timestamp =  new DateTime(2001, 1, 1, 0, 0, 0,DateTimeZone.forID("CET")).toDate,
    sparkNotebook = Some(sparkNotebook),
    customLocalRepo = customLocalRepo,
    customRepos = customRepos,
    customDeps = customDeps,
    customImports = customImports,
    customArgs = customArgs,
    customSparkConf = customSparkConf)

  val raw =
    """{
      |"metadata" : {
      |  "id" : "foo-bar-loo-lar",
      |  "name" : "test-notebook-name",
      |  "user_save_timestamp" : "1999-09-09T09:09:09.000Z",
      |  "auto_save_timestamp" : "2001-01-01T00:00:00.000Z",
      |  "language_info" : {
      |    "name" : "scala",
      |    "file_extension" : "scala",
      |    "codemirror_mode" : "text/x-scala"
      |  },
      |  "trusted" : true,
      |  "sparkNotebook" : {
      |    "build" : "unit-tests"
      |  },
      |  "customLocalRepo" : "local-repo",
      |  "customRepos" : [ "custom-repo" ],
      |  "customDeps" : [ "\"org.custom\" % \"dependency\" % \"1.0.0\"" ],
      |  "customImports" : [ "import org.cusom.dependency.SomeClass" ],
      |  "customArgs" : [ ],
      |  "customSparkConf" : {
      |    "spark.driverPort" : 1234
      |  },
      |  "customVars" : null
      |},
      |"cells" : [ ]
      |}
    """.stripMargin

  val notebook = {
    val nb = Notebook(metadata = Some(metadata), nbformat = None)
    val normalizedNb = Notebook.serializeFuture(nb).flatMap(Notebook.deserializeFuture)
    Await.result(normalizedNb, DefaultWaitSeconds)
  }

  override def beforeAll: Unit = {
    tempPath = Files.createTempDirectory("file-system-notebook-provider")
    notebookPath = tempPath.resolve("notebooks")
    Files.createDirectories(notebookPath)

    val dirConfig = ConfigFactory.parseMap(Map("notebooks.dir" -> notebookPath.toAbsolutePath.toString).asJava)
    val configurator = new FileSystemNotebookProviderConfigurator()
    provider = Await.result(configurator(dirConfig), DefaultWaitSeconds)
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory( tempPath.toFile )
  }

  "File system notebook provider" should {

    "create a notebook file" in {
      val nbPath = notebookPath.resolve("testNew.snb")
      assume(!nbPath.toFile.exists())
      whenReady( provider.save(nbPath, notebook) ) { n =>
        nbPath.toFile.exists() should be (true)
      }
    }

    "created content should be valid JSON" in {
      val nbPath = notebookPath.resolve("testJson.snb")
      whenReady( provider.save(nbPath, notebook) ) { n =>
        val content = Source.fromFile(nbPath.toFile).mkString("")
        Try{Json.parse(content)} should be ('success)
      }
    }

    "load saved file" in {
      val nbPath = notebookPath.resolve("testLoad.snb")
      val loadedNb = for {
        _ <- provider.save(nbPath, notebook)
        loaded <- provider.get(nbPath)
      } yield loaded

      whenReady( loadedNb ) { nb =>
        nb should be (notebook)
      }
    }

    "move a notebook file to another directory" in {
      val nbName = "testMove.snb"
      val originalPath = notebookPath.resolve(nbName)
      val targetDir = notebookPath.resolve(s"dest")
      Files.createDirectories(targetDir)
      val targetNbPath = targetDir.resolve(nbName)

      val moved = for {
        _ <- provider.save(originalPath, notebook)
        path <- provider.move(originalPath, targetNbPath)
        nb <- provider.get(path)
      } yield (nb, path)

      whenReady(moved) { case (nb, path) =>
        path should be (targetNbPath)
        originalPath.toFile.exists() should be(false)
        path.toFile.exists() should be(true)
        nb should be (notebook)
      }
    }

    "rename a notebook file" in {
      val originalPath = notebookPath.resolve("testRename.snb")
      val newName = "renamed.snb"
      val destPath = notebookPath.resolve(newName)

      val renamedNotebook = for {
        _ <- provider.save(originalPath, notebook)
        newPath <- provider.move(originalPath, destPath)
        renamedNb <- provider.get(newPath)
      } yield renamedNb

      whenReady (renamedNotebook) {nb =>
        originalPath.toFile.exists() should be (false)
        destPath.toFile.exists() should be (true)
        nb.name should be (newName)
        nb.metadata.get.name should be (newName)
      }
    }

    "preserve the id of a renamed notebook" in {
      val nbPath = notebookPath.resolve("testinternal-rename.snb")
      val originalId = notebook.metadata.get.id
      val res = for {
        nb <- provider.save(nbPath, notebook)
        _ <- provider.renameInternal(nbPath, "testinternal-renamed")
        renamed <- provider.get(nbPath)
      } yield renamed

      whenReady(res) { renamedNb =>
        renamedNb.metadata.get.id should be(originalId)
      }
    }

    "fail to move an unexisting notebook" in {
      whenReady( provider.move(Paths.get("/path/to/nowhere"), notebookPath).failed ) { n =>
        n shouldBe a [java.lang.IllegalArgumentException]
      }
    }

    "fail to move a notebook to a non-existing directory" in {
      val nbPath = notebookPath.resolve("testMoveNonExist.snb")
      val nonExistingPath = notebookPath.resolve("nowhere/testMoveNonExist.snb")
      val test = for {
        nb <- provider.save(nbPath, notebook)
        moved <- provider.move(nbPath, nonExistingPath)
      }  yield (moved)

      whenReady( test.failed ) { failure =>
        failure shouldBe a[java.lang.IllegalArgumentException]
      }
    }

    "fail to load an unexisting file" in {
      whenReady( provider.get(Paths.get("/path/to/nowhere")).failed ) { n =>
        n shouldBe a [java.nio.file.NoSuchFileException]
      }
    }

    "delete the file" in {
      val nbPath = notebookPath.resolve("testDeleted.snb")
      val deletedNb = for {
        _ <- provider.save(nbPath,notebook)
        deleted <- provider.delete(nbPath)
      } yield deleted

      whenReady( deletedNb ) { n =>
        nbPath.toFile.exists() should be (false)
      }
    }

    "fail to load deleted file" in {
      val nbPath = notebookPath.resolve("notThere.snb")
      whenReady( provider.get(nbPath).failed ) { n =>
        n shouldBe a [java.nio.file.NoSuchFileException]
      }
    }

  }

}
