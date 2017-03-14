package notebook.io

import java.nio.file.Files
import java.util.{Date, Properties}

import com.typesafe.config.ConfigFactory
import notebook.NotebookNotFoundException
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import org.eclipse.jgit.api.Git


class GitNotebookProviderNoCloneTests extends TestBase {

  import TestData._
  import scala.collection.JavaConverters._

  val notebook = TestData.notebook()

  override def beforeAll: Unit = {
    val providerConfig = new GitNotebookProviderConfigurator
    emptyTemp = Files.createTempDirectory("git-no-clone-tests")
    val conf = Map("local_path" -> emptyTemp.toString)
    setProvider(providerConfig(ConfigFactory.parseMap(conf.asJava)))

    target = emptyTemp.resolve(s"$testName.snb")
  }

  override def afterAll: Unit = {
     FileUtils.deleteDirectory( emptyTemp.toFile )
  }

  "Git notebook provider" should {
    import collection.JavaConversions._
    def lastLog():String = gitNotebookProvider.gitProvider.gitContext.git.log().call().iterator().toIterable.head.getFullMessage

    "create a repository" in {
      emptyTemp.toFile.listFiles().toList should not be ('empty)
    }

    "create a notebook file" in {
      whenReady(provider.save(target, notebook)) { n =>
        n should be(notebook)
        lastLog shouldBe (s"${CommitMessagesConfiguration.DefaultCommitMessage} ${testName}.snb")
      }
    }

    "load created file" in {
      whenReady( provider.get(target) ) { n =>
        n should be (notebook)
      }
    }

    "delete the file" in {
      whenReady( provider.delete(target) ) { n =>
        n should be (notebook)
        lastLog shouldBe(s"${CommitMessagesConfiguration.DefaultDeleteMessage} ${testName}.snb")
      }
    }

    "fail to load deleted file" in {
      whenReady( provider.get(target).failed ) { n =>
        n shouldBe a [NotebookNotFoundException]
      }
    }

  }
}
