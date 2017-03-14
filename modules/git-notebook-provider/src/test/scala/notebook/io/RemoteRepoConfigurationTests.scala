package notebook.io

import java.net.URI
import java.nio.file.{Files, Path}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.scalatest._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._


class RemoteRepoConfigurationTests extends WordSpec with Matchers with OptionValues with TryValues  with BeforeAndAfterAll {

  val remoteRepoURI = "ssh://some.remote.com"

  var testDir: Path = _

  override def beforeAll: Unit = {
    testDir = Files.createTempDirectory("remote-repo-test")
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory( testDir.toFile )
  }

  def localRepo ={
    val config = ConfigFactory.parseMap(Map(LocalRepoConfiguration.LocalPathKey -> testDir.toString).asJava)
    val eventualRepo = for {
      commitMessages <- CommitMessagesConfiguration(config)
      localRepo <- FutureUtil.tryToFuture(LocalRepoConfiguration.parse(config, commitMessages))
    } yield {localRepo}
      Await.result(eventualRepo, 5 seconds)
  }


  "RemoteRepoConfiguration" should {

    "handle SSH repo URL" in {
      RemoteRepoConfiguration.validateRepoUrl(new URI("ssh://user@example.com/repo.git")) shouldBe ('success)
    }

    "handle HTTP repo URL" in {
      RemoteRepoConfiguration.validateRepoUrl(new URI("http://user@example.com/repo.git")) shouldBe ('success)
    }

    "handle HTTPS repo URL" in {
      RemoteRepoConfiguration.validateRepoUrl(new URI("https://user@example.com/repo.git")) shouldBe ('success)
    }

    "fail to handle unsupported repo protocol" in {
      val res = RemoteRepoConfiguration.validateRepoUrl(new URI("mqtt://user@example.com/repo.git"))
      res shouldBe ('failure)
      res.failed.get shouldBe an [ConfigurationCorruptException]
    }

    "correctly parse a missing remote configuration" in {
      val config = ConfigFactory.empty()
      RemoteRepoConfiguration.parse(config).success.value should be (None)
    }

    def configFrom(map:Map[String,String]):Config = ConfigFactory.parseMap(map.asJava)

    "require further configuration if remote is present" in {
      val config = configFrom(Map("remote" -> "ssh://some.remote.com"))
      RemoteRepoConfiguration.parse(config).failure.exception shouldBe an [ConfigurationCorruptException]
    }

    "use a user/password combination " in {
      val config = configFrom(Map("remote" -> remoteRepoURI,
        "authentication.username" -> "user", "authentication.password" -> "pwd"
      ))
      val remoteRepoFunc = RemoteRepoConfiguration.parse(config).success.value.value
      val remoteRepo = remoteRepoFunc(localRepo)
      remoteRepo.remote.toString should be (remoteRepoURI)
      remoteRepo.auth shouldBe an[UsernamePasswordAuth]
    }

    "use a password" in {
      val config = configFrom(Map("remote" -> remoteRepoURI,
        "authentication.password" -> "pwd"
      ))
      val remoteRepoFunc = RemoteRepoConfiguration.parse(config).success.value.value
      val remoteRepo = remoteRepoFunc(localRepo)
      remoteRepo.remote.toString should be (remoteRepoURI)
      remoteRepo.auth shouldBe an [PasswordAuth]
    }

  }

}
