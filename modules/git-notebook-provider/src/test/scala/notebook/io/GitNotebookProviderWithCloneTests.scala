package notebook.io

import java.nio.file.{Files, Path, Paths}
import java.util.Properties

import com.typesafe.config.ConfigFactory
import notebook.NotebookNotFoundException
import org.apache.commons.io.FileUtils
import org.scalatest.Ignore

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import notebook.io.RemoteGitTestConfig._

object RemoteGitTestConfig {
  protected def getEnv(name: String): String = {
    require(sys.env.isDefinedAt(name), s"Missing ENV var '$name'")
    sys.env(name)
  }

  def getRemoteGitConfig(authType: String): Option[Properties] = {
    authType match {
      case "https" =>
        val props = new Properties()
        props.setProperty("remote", getEnv("TEST_GIT_REPO_HTTPS"))
        props.setProperty("authentication.username", getEnv("TEST_GIT_USER"))
        props.setProperty("authentication.password", getEnv("TEST_GIT_PASS"))
        Option(props)
      case "ssh" =>
        val props = new Properties()
        props.setProperty("remote", getEnv("TEST_GIT_REPO_SSH"))
        props.setProperty("authentication.key_file", getEnv("TEST_GIT_KEYFILE"))
        // FIXME: also test without keyfile passphare (as it is optional)
        props.setProperty("authentication.key_file_passphrase", getEnv("TEST_GIT_KEYFILE_PASS"))
        Option(props)
      case _ => None
    }

  }
}

@Ignore
class GitNotebookProviderCloneHttpsTests extends TestBase {

  import TestData._

  def remoteGitConfig = getRemoteGitConfig("https")

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))
  val testId = System.currentTimeMillis()
  var props: Properties = _
  val providerConfigurator = new GitNotebookProviderConfigurator
  val notebook = TestData.notebook()

  override def beforeAll: Unit = {
    emptyTemp = Files.createTempDirectory("git-https-notebook-provider")
    nonEmptyTemp = Files.createTempDirectory("git-https-notebook-provider-non-empty")

    props = remoteGitConfig.get
    props.setProperty("local_path", emptyTemp.toString)
    props.setProperty("messages.save", s"[test-id:https:$testId] saved:")
    props.setProperty("messages.remove", s"[test-id:https:$testId] removed:")

    setProvider(providerConfigurator(ConfigFactory.parseProperties(props)))

    target = emptyTemp.resolve(s"$testName-https.snb")
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory( emptyTemp.toFile )
  }

  s"HTTPS clone provider [test-id:https:$testId]" should {

    "have the repository cloned in the temp directory" in {
      val files = emptyTemp.toFile.listFiles().toList
      files should not be ('empty)
      files.exists(file=> file.getName == ".git") should be (true)
    }

    "create a notebook file" in {
      whenReady( provider.save(target, notebook) ) { n =>
        n shouldBe(notebook)
      }
    }

    "load created file" in {
      whenReady( provider.get(target) ) { n =>
        n shouldBe(notebook)
      }
    }

    "not load a non existing file" in {
      whenReady( provider.get(Paths.get("/some/bogus/path")).failed ) { n =>
        n shouldBe a [NotebookNotFoundException]
      }
    }

    "delete the file" in {
      assume(target.toFile.exists())
      whenReady( provider.delete(target) ) { n =>
        n shouldBe ()
        target.toFile.exists() shouldBe (false)
      }
    }

    "fail to load deleted file" in {
      whenReady( provider.get(target).failed ) { n =>
        n shouldBe a [NotebookNotFoundException]
      }
    }

    "reuse an already cloned repository cloned in the temp directory" in {
      val otherProviderConf = new GitNotebookProviderConfigurator
      val otherProvider = Await.result(otherProviderConf(ConfigFactory.parseProperties(props)), DefaultWaitSeconds)
      otherProvider.asInstanceOf[ConfigurableGitNotebookProvider]
        .gitProvider.gitContext.branch shouldBe (GitNotebookProvider.DefaultBranch)
    }
  }
}

@Ignore
class GitNotebookProviderCloneSshTests extends TestBase {

  import TestData._

  def remoteGitConfig = getRemoteGitConfig("ssh")

  implicit val defaultPatience = PatienceConfig(timeout = Span(10, Seconds), interval = Span(500, Millis))
  val testId = System.currentTimeMillis()
  val providerConfigurator = new GitNotebookProviderConfigurator
  val notebook = TestData.notebook()

  override def beforeAll: Unit = {
    emptyTemp = Files.createTempDirectory("git-ssh-notebook-provider")
    target = emptyTemp.resolve(s"$testName-ssh.snb")
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory(emptyTemp.toFile)
  }

  s"SSH clone provider [test-id:ssh:$testId]" should {
    "have the repository cloned in the temp directory" in {
      assume(emptyTemp.toFile.list().isEmpty)

      val props = remoteGitConfig.get
      props.setProperty("local_path", emptyTemp.toString)
      props.setProperty("branch", "master")
      props.setProperty("messages.save", s"[test-id:ssh:$testId] saved:")
      props.setProperty("messages.remove", s"[test-id:ssh:$testId] removed:")

      setProvider(providerConfigurator(ConfigFactory.parseProperties(props)))

      gitNotebookProvider.gitProvider.gitContext.branch shouldBe(GitNotebookProvider.DefaultBranch)
      emptyTemp.toFile.list().toList should not be ('empty)
    }

    "create a notebook file" in {
      whenReady( provider.save(target, notebook) ) { n =>
        n shouldBe(notebook)
      }
    }

    "load created file" in {
      whenReady( provider.get(target) ) { n =>
        n shouldBe(notebook)
      }
    }

    "delete the file" in {
      whenReady( provider.delete(target) ) { n =>
        n shouldBe ()
      }
    }

    "fail to load deleted file" in {
      whenReady( provider.get(target).failed ) { n =>
        n shouldBe a [NotebookNotFoundException]
      }
    }

  }

}

@Ignore
class GitNotebookProviderCloneSshWithGithubStyleURI extends GitNotebookProviderCloneSshTests {
  // pass a Github style repo URL: git@github.com:organization/repo-name.git
  override def remoteGitConfig() = {
    super.remoteGitConfig.map { props =>
      val githubStyleRemote = props.getProperty("remote").replace("ssh://git@github.com/", "git@github.com:")
      println(s"Using Git remote: $githubStyleRemote")
      props.setProperty("remote", githubStyleRemote)
      props
    }
  }
}
