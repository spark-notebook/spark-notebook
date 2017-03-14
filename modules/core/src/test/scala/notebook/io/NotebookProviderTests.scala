package notebook.io

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.io.File

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.io.FileUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.prop.Tables.Table
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import notebook.{GenericFile, Notebook, NotebookResource, Repository}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class NotebookProviderTests extends WordSpec with Matchers with BeforeAndAfterAll with ScalaFutures with TableDrivenPropertyChecks {

  implicit val defaultPatience =
    PatienceConfig(timeout =  Span(2, Seconds), interval = Span(5, Millis))

  val rootPath = Files.createTempDirectory("notebook-provider-test")
  val notebookPath = rootPath.resolve("notebook")
  val emptyPath = rootPath.resolve("empty")
  val notebookFile = "nb.snb"
  val resourceFile = "res.res"
  val dir = "dir"
  val hiddenDir = ".hidden"
  val hiddenGitDir = ".git"
  val dirPath = notebookPath.resolve(dir)
  val hiddenPath = notebookPath.resolve(hiddenDir)
  val hiddenGitPath = notebookPath.resolve(hiddenGitDir)

  override def beforeAll(): Unit = {
    Seq(notebookPath, emptyPath, dirPath, hiddenPath, hiddenGitPath).foreach(path => Files.createDirectory(path))
    Seq(notebookFile, resourceFile).foreach { file =>
      val filePath = notebookPath.resolve(file)
      Files.write(filePath, "text".getBytes(StandardCharsets.UTF_8))
    }
  }

  override def afterAll(): Unit = {
    FileUtils.deleteDirectory(rootPath.toFile)
  }

  class BaseProvider extends NotebookProvider {
    type SaveSpec = Unit
    override def root: Path = rootPath
    override def get(path: Path, version: Option[Version])(implicit ev: ExecutionContext): Future[Notebook] = ???
    override def delete(path: Path)(implicit ev: ExecutionContext): Future[Notebook] = ???
    override def save(path: Path, notebook: Notebook, saveSpec:Option[String] = None)(implicit ev: ExecutionContext): Future[Notebook] = ???
    override def moveInternal(src: Path, dest: Path)(implicit ev: ExecutionContext): Future[Path] = ???
  }

  val T = true
  val F = false

  val providerDefault = new BaseProvider()

  val providerNoFilter = new BaseProvider(){
    override val listingPolicy = (f:File) => true
  }

  val providerExplicitFilter = new BaseProvider() {
    override val listingPolicy = (f:File) => (f.getName endsWith ".snb") || (f.getName startsWith "res")
  }

  val providerPatternFilter = new BaseProvider() {
    override val listingPolicy = (f:File) =>
      if (f.isDirectory) {
        !f.getName.startsWith(".")
      } else {
        f.getName.endsWith(".snb")
    }
  }

  "Notebook provider" should {

    "retrieve a list of resources from an empty path" in {
      whenReady(providerDefault.list(emptyPath)) { files =>
        files should be('empty)
      }
    }


    "retrieve a list of resources from a path" in {
      whenReady(providerNoFilter.list(notebookPath)) { resources =>
        resources.size should be(5)
        resources.foreach {
          case nb: NotebookResource => nb.name should be(notebookFile.dropRight(".snb".size))
          case res: GenericFile => res.name should be(resourceFile)
          case rep: Repository => rep.name should (be(hiddenDir) or be(dir) or be(hiddenGitDir))
          case x => fail("unexpected resource" + x)
        }
      }
    }

    "retrieves a list of resources using default filtering" in {
      whenReady(providerDefault.list(notebookPath)) { resources =>
        resources.size should be (2)
        resources.foreach {
          case nb: NotebookResource => nb.name should be(notebookFile.dropRight(".snb".size))
          case rep: Repository => rep.name should be(dir)
          case x => fail("unexpected resource" + x)
        }
      }

    }
  }

  "Notebook provider" should {

    val defaultInstance = new BaseProvider()

    "retrieves a list of resources filtered with a literal" in {
      whenReady(providerExplicitFilter.list(notebookPath)) { resources =>
        println(" Retrieved files: " + resources.mkString(", "))
        resources.size should be (2)
        resources.foreach {
          case nb: NotebookResource => nb.name should be(notebookFile.dropRight(".snb".size))
          case res: GenericFile => res.name should be(resourceFile)
          case x => fail("unexpected resource" + x)
        }
      }
    }

    "retrieves a list of resources filtered with a pattern" in {
      whenReady(providerPatternFilter.list(notebookPath)) { resources =>
        resources.size should be (2)
        resources.foreach {
          case nb: NotebookResource => nb.name should be(notebookFile.dropRight(".snb".size))
          case rep: Repository => rep.name should be(dir)
          case x => fail("unexpected resource" + x)
        }
      }
    }

  }

  val filterTable = Table(
    ("filename", "no_filter", "explicit_filter", "pattern_filter"),
    (".git",     T, F, F),
    ("res.res",  T, T, F),
    ("nt.snb",   T, T, T),
    (".file",    T, F, F)
  )
  val withProvider: BaseProvider => String => Boolean = provider => filename =>
    provider.listFilter.accept(new java.io.File(s"./$filename"))

  "allow filenames following the filter criteria" in {
    val explicitFiltering = withProvider(providerExplicitFilter)
    val noFiltering = withProvider(providerNoFilter)
    val patternFiltering = withProvider(providerPatternFilter)

    forAll(filterTable) { (filename, noFilter, explicitFilter, patternFilter) =>
      explicitFiltering(filename) should be(explicitFilter)
      noFiltering(filename) should be(noFilter)
      patternFiltering(filename) should be(patternFilter)
    }
  }

}
