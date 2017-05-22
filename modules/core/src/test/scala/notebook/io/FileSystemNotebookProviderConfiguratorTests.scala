package notebook.io

import java.nio.file.{Files, Path}

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject}
import scala.collection.JavaConverters._
import org.scalatest.time.{Millis, Seconds, Span}

class FileSystemNotebookProviderConfiguratorTests extends WordSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))

  var notebookDir : String = _
  var tempDir: Path = _

  val notebooksDirName = "notebook"

  override def beforeAll: Unit = {
    tempDir = Files.createTempDirectory("file-system-notebook-provider")
    notebookDir = s"${tempDir.toAbsolutePath.toFile.getAbsolutePath}/$notebooksDirName"
  }

  "File system notebook provider configurator" should {

    def instantiateProvider() = Class.forName("notebook.io.FileSystemNotebookProviderConfigurator").newInstance().asInstanceOf[Configurable[NotebookProvider]]

    "be instantiable" in {
      noException should be thrownBy (instantiateProvider)
    }

    "configure a new notebook provider" in {
      val configurator = instantiateProvider()
      val dirConfig = ConfigFactory.parseMap(Map("notebooks.dir" -> notebookDir).asJava)
      val notebookProvider = configurator(dirConfig)
      whenReady(notebookProvider) { nbp =>
        nbp shouldBe a[NotebookProvider]
      }
    }

    "accepts and cleans up non-cannonical relative paths" in {
      val nonCanonicalNotebooksDir = notebookDir + s"/../$notebooksDirName"

      val configurator = instantiateProvider()
      val dirConfig = ConfigFactory.parseMap(Map("notebooks.dir" -> nonCanonicalNotebooksDir).asJava)
      val notebookProvider = configurator(dirConfig)
      whenReady(notebookProvider) { nbp =>
        nbp shouldBe a[NotebookProvider]
        // returns a root without notebook/../notebook
        // (just ignore OSx tmpdir prefix '/private' for now)
        nbp.root.toString.stripPrefix("/private") shouldBe notebookDir
      }
    }

    "fail when the configuration is missing" in {
      val configurator = instantiateProvider()
      val dirConfig = ConfigFactory.parseMap(Map("foo.bar" -> "boo").asJava)
      val notebookProvider = configurator(dirConfig)
      whenReady(notebookProvider.failed) { nbp =>
        nbp shouldBe a[ConfigurationMissingException]
        nbp.getMessage should include (FileSystemNotebookProviderConfigurator.NotebooksDir)
      }
    }

  }

}
