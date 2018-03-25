package notebook.server

import java.io._
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.text.SimpleDateFormat
import java.util.Date

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import notebook.NBSerializer._
import notebook.io.Version
import notebook.{Notebook, Resource}
import org.apache.commons.io.FileUtils
import play.api.Logger
import play.api.libs.json._
import utils.AppUtils
import utils.Const.UTF_8

class NotebookManager(val notebookConfig: NotebookConfig) {

  val provider = notebookConfig.notebookIoProvider

  val DefaultOperationTimeout = 60.seconds

  val notebookDir: File = provider.root.toFile

  Logger.info("Notebook directory is: " + notebookDir.getCanonicalPath)

  private lazy val config = AppUtils.notebookConfig
  val viewer = config.viewer

  val extension = Notebook.EXTENSION_SNB_IPYNB

  val name = notebookConfig.projectName // TODO: Whatever uses this name, should be using config.projectName directly, for sure...

  def forbidInViewer[T](block: => T): T = {
    if (!viewer){
      block
    } else {
      throw new IllegalStateException("This action is not allowed in viewer mode")
    }
  }

  def getName(path: String) = {
    val fileName = path.split("/").filter(!_.isEmpty).last
    Notebook.notebookName(fileName)
  }

  def notebookFile(path: String): File = {
    val basePath = notebookDir.getCanonicalPath
    val decodedPath = URLDecoder.decode(path, UTF_8)
    val nbFile = new File(basePath, decodedPath)
    nbFile
  }

  def incrementFileName(base: String): String = {
    Logger.info("Incremented Notebook at " + base)
    val newPath: String = Stream.from(1).map(base + _ + extension).dropWhile { fn =>
      val snb = notebookFile(fn)
      val r = snb.exists()
      Logger.info(s"SNB ${snb.getAbsolutePath} exists: $r")
      r
    }.head

    Logger.info("Incremented Notebook is " + newPath)

    newPath
  }

  def newNotebook(
    path: String = "/",
    customLocalRepo: Option[String] = None,
    customRepos: Option[List[String]] = None,
    customDeps: Option[List[String]] = None,
    customImports: Option[List[String]] = None,
    customArgs: Option[List[String]] = None,
    customSparkConf: Option[JsObject] = None,
    name:Option[String] = None): String = forbidInViewer {
    val sep = if (path.last == '/') "" else "/"
    val fpath = name.map(path + sep + _ + extension).getOrElse(incrementFileName(path + sep + "Untitled"))
    val nb = Notebook(
      metadata = Some(new Metadata(
        id = Notebook.getNewUUID,
        name = getName(fpath),
        customLocalRepo = customLocalRepo,
        customRepos = customRepos,
        customDeps = customDeps,
        customImports = customImports,
        customArgs = customArgs,
        customSparkConf = customSparkConf)),
      Some(Nil),
      None,
      None,
      None
    )
    save(fpath, nb, overwrite = false)
    fpath
  }

  def copyNotebook(nbPath: String): String = forbidInViewer {
    val nbData = getNotebook(nbPath)
    nbData.map { nb =>
      val newPath = incrementFileName(Notebook.notebookName(nb.path))
      val newName = getName(newPath)
      val readExistingAndSaveNewNb: Future[String] = Notebook.deserializeFuture(nb.data).map { oldNB =>
        val newNb = Notebook(oldNB.metadata.map(_.copy(id = Notebook.getNewUUID, name = newName)), oldNB.cells, oldNB.worksheets, oldNB.autosaved, oldNB.nbformat, oldNB.nbformat_minor)
        save(newPath,newNb, overwrite = false)
        newPath
      }
      Await.result(readExistingAndSaveNewNb, DefaultOperationTimeout)
    } getOrElse newNotebook()
  }

  def listResources(path: String): List[Resource] = {
    Logger.info(s"listNotebooks at path $path")
    Await.result(provider.list(absolutePath(path)), DefaultOperationTimeout)
  }

  /**
    * Creates a new directory with name "name" under the provided path relative to this server's root directory
    *
    * @param parent the relative path that will be parent to the new directory
    * @param name the name of the new directory
    * @return a string representing the new path relative to the parent
    */
  def mkDir(parent:String, name:String): Try[String] = {
    val base = new File(notebookDir, parent)
    val newDir = new File(base, name)
    if (newDir.mkdirs()) {
      Success(newDir.getAbsolutePath.diff(base.getAbsolutePath))
    } else {
        Failure(new DirectoryCreationException(newDir))
    }
  }

  def getNotebook(path: String) = {
    Logger.info(s"getNotebook at path $path")
    for (notebook <- load(path)) yield {
      val data = FileUtils.readFileToString(notebookFile(path), "UTF-8")
      val lastModified = NotebookInfo.formatTimestamp(notebookFile(path).lastModified())
      NotebookInfo(lastModified, notebook.name, data, path)
    }
  }

  def deletePath(path: String): Future[Unit] = forbidInViewer {
    Logger.info(s"deleteNotebook at path $path")
    provider.delete(absolutePath(path))
  }

  def rename(srcPath: String, destPath: String): (String, String) = forbidInViewer {
    Logger.info(s"rename from path $srcPath to $destPath")
    val newname = getName(destPath)
    val src = absolutePath(srcPath)
    val dest = absolutePath(destPath)
    val resPath = Await.result(provider.move(src, dest), DefaultOperationTimeout)
    (newname, destPath)
  }

  def save(path: String, notebook: Notebook, overwrite: Boolean, message: Option[String]= None): (String, String) = forbidInViewer {
    Logger.info(s"save at path $path with message $message")
    val file = notebookFile(path)
    if (!overwrite && file.exists()) {
      throw new NotebookExistsException(s"Notebook [$path] already exists.")
    }
    // TODO: this is expected to be sync, and the implementation is ugly...

    // TODO: either `provider.save` takes an extra message parameter which changes its behavior for git (commit)
    // or we have a dedicated function like `checkpoint` or `commit` for that purpose and `save` is just saving the file
    val savedNb: Notebook = Await.result(provider.save(Paths.get(file.toURI), notebook, message), DefaultOperationTimeout)
    (savedNb.metadata.get.name, path)
  }

  def checkpoints(path: String): List[Version] = {
    Await.result(provider.versions(absolutePath(path)), DefaultOperationTimeout)
  }

  def restoreCheckpoint(path:String, id:String): Option[(String, String, String)] = {
    val notebookCheckpoint =  provider.get(absolutePath(path), Some(Version(id,"", 1L))).map{ nb =>
      val lastModifiedTime: String = NotebookInfo.formatTimestamp(nb.metadata.get.user_save_timestamp)
      Some((lastModifiedTime , nb.name, path))
    }
    Await.result(notebookCheckpoint, DefaultOperationTimeout)
  }

  def load(path: String): Option[Notebook] = {
    Logger.info(s"Loading notebook at path $path")
    // TODO: this is expected to be sync, and the implementation is ugly...
    val notebook: Future[Option[Notebook]] = provider.get(absolutePath(path)).map(Option.apply)
    Await.result(notebook, DefaultOperationTimeout)
      .orElse(None)
  }

  protected def absolutePath(path: String): Path = {
    Paths.get(notebookFile(path).toURI)
  }

}

class NotebookExistsException(message: String) extends IOException(message)
class DirectoryCreationException(path: File) extends Exception("Could not create dir at: " + path.getCanonicalPath)

case class NotebookInfo(lastModified: String, name: String, data: String, path: String)

object NotebookInfo {
  def formatTimestamp(timestamp: Date): String = {
    new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'").format(timestamp)
  }

  def formatTimestamp(timestampMillis: Long): String = {
    formatTimestamp(new Date(timestampMillis))
  }
}
