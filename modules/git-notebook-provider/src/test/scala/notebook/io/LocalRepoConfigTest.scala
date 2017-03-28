package notebook.io

import java.nio.file.{Files, Path}

import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, TryValues, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import FutureUtil._
import org.scalatest.time.{Millis, Seconds, Span}

class LocalRepoConfigTest extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures with TryValues {

  var testDir: Path = _

  implicit val defaultPatience =  PatienceConfig(timeout = Span(5, Seconds), interval = Span(50, Millis))

  override def beforeAll: Unit = {
    testDir = Files.createTempDirectory("local-repo")
  }

  "LocalRepoConfig" should {
    "use the directory in the configuration" in {
      val config = ConfigFactory.parseMap(Map(LocalRepoConfiguration.LocalPathKey -> testDir.toString).asJava)
      val eventualLocalRepo = for {
        commitMessages <- CommitMessagesConfiguration(config)
        localRepo <- tryToFuture(LocalRepoConfiguration.parse(config, commitMessages))
      } yield localRepo

      whenReady(eventualLocalRepo) { localRepo =>
        localRepo.pathState shouldBe a[EmptyPath]
      }
    }

    "fail with a missing configuration" in {
      val config = ConfigFactory.empty()
      val eventualLocalRepo = for {
        commitMessages <- CommitMessagesConfiguration(config)
        localRepoConfiguration <- tryToFuture(LocalRepoConfiguration.parse(config, commitMessages))
      } yield localRepoConfiguration

      whenReady(eventualLocalRepo.failed) { failedRepoConf =>
        failedRepoConf match {
          case ex: ConfigurationMissingException => ex.key should be(LocalRepoConfiguration.LocalPathKey)
          case x => fail("Non compliant exception:" + x)
        }
      }
    }
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory( testDir.toFile )
  }

}
