package notebook.io

import java.io.File
import java.nio.file.{Files, Path, Paths}

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source



class LocalGitProviderTests extends WordSpec with Matchers with BeforeAndAfterAll {

  var emptyTemp: Path = _
  var localGitProvider: LocalGitProvider = _

  override def beforeAll(): Unit = {
    emptyTemp = Files.createTempDirectory("local-git-tests")
    localGitProvider = new TestLocalGitProvider(emptyTemp)
    Await.ready(localGitProvider.init(), 5 seconds)
    println ("using dir: " + emptyTemp)
  }

  class TestGitContext(testDir: Path) extends GitContext {

    val repository: Repository = new FileRepositoryBuilder().setWorkTree(testDir.toFile).build()

    override def git: Git = new Git(repository)

    override def commitMessages: CommitMessages = CommitMessages("Initial commit", "delete bum", "committed like a pig", "moved around")

    override def branch: String = "master"

    override def localPath: Path = testDir
  }


class TestLocalGitProvider(testDir:Path) extends TestGitContext(testDir) with LocalGitProvider { ctx: GitContext =>

  override def init()(implicit ec: ExecutionContext): Future[Unit] = Future {
    Git.init().setBare(false).setDirectory(testDir.toFile).call()
  }
  override def gitContext: GitContext = ctx

}

  "LocalGitProvider" should {
    "commit a file with a commit message" in {
      val filename = "afile.txt"
      val commitMsg = "commit message"
      fileWithContent(filename)("Some Data")

      val fVersions = for {
        _ <- localGitProvider.add(filename, commitMsg)
        versions <- localGitProvider.versions(filename)
      } yield versions
      val versions = Await.result(fVersions, 10 seconds)

      versions.size should be (1)
      versions.head.message should be (commitMsg)
    }

    "checkout a given version" in {
      val filename = "somefile.txt"

      val adds: Future[Path] = for {
        file <- Future(fileWithContent(filename)("Some Data"))
        _ <- localGitProvider.add(filename, "first commit")
        _ <- Future(fileWithContent(filename)("Some More Data"))
        _ <- localGitProvider.add(filename, "second commit")
        _ <- Future(fileWithContent(filename)("Error"))
        _ <- localGitProvider.add(filename, "third commit")
      } yield (file)

      val file = Await.result(adds, 10 seconds)

      val futVersions = localGitProvider.versions(filename)
      val versions = Await.result(futVersions, 2 seconds)

      val pickVersion = versions.drop(1).head
      assume(pickVersion.message == "second commit")

      Await.ready(localGitProvider.checkout(filename, pickVersion.id), 5 seconds)

      val content = Source.fromFile(file.toFile).getLines().mkString("")
      content should be ("Some More Data")
    }
  }


  def fileWithContent(filename: String)(content:String = ""): Path = {
    val file = emptyTemp.resolve(filename)
    Files.write(file, content.getBytes("UTF-8"))
    file
  }


}
