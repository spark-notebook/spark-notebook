package notebook.server

import java.io._
import java.nio.charset.{Charset, StandardCharsets}
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date

import notebook.Notebook
import notebook.NBSerializer._
import org.apache.commons.io.FileUtils
import play.api.Logger
import play.api.libs.json._
import utils.Const.UTF_8

class NotebookManager(val name: String, val notebookDir: File) {

  Logger.info("Notebook directory is: " + notebookDir.getCanonicalPath)

  val extension = ".snb"

  def getName(path: String) = path.split("/").filter(!_.isEmpty).last.dropRight(extension.length)

  def notebookFile(path: String): File = {
    val basePath = notebookDir.getCanonicalPath
    val decodedPath = URLDecoder.decode(path, UTF_8)
    val nbFile = new File(basePath, decodedPath)
    nbFile
  }

  def incrementFileName(base: String) = {
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
    name:Option[String] = None) = {
    val sep = if (path.last == '/') "" else "/"
    val fpath = name.map(path + sep + _ + extension).getOrElse(incrementFileName(path + sep + "Untitled"))
    val nb = Notebook(
      Some(Metadata(
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

  def copyNotebook(nbPath: String) = {
    val nbData = getNotebook(nbPath)
    nbData.map { nb =>
      val newPath = incrementFileName(nb._4.dropRight(extension.length))
      val newName = getName(newPath)
      Notebook.deserialize(nb._3) match {
        case Some(oldNB) =>
          val newMeta = oldNB.metadata.map(_.copy(id = Notebook.getNewUUID, name = newName))
            .orElse(Some(Metadata(id = Notebook.getNewUUID, name = newName)))
          save(newPath, Notebook(newMeta, oldNB.cells, oldNB.worksheets, oldNB.autosaved, None), false)
          newPath
        case None =>
          newNotebook()
      }
    } getOrElse newNotebook()
  }

  def getNotebook(path: String) = {
    Logger.info(s"getNotebook at path $path")
    for (notebook <- load(path)) yield {
      val data = FileUtils.readFileToString(notebookFile(path), "UTF-8")
      val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'")
      val last_mtime = df.format(new Date(notebookFile(path).lastModified()))
      (last_mtime, notebook.name, data, path)
    }
  }

  def deleteNotebook(path: String) = {
    Logger.info(s"deleteNotebook at path $path")
    val file = notebookFile(path)
    if (file.exists()) {
      file.delete()
    }
  }

  def rename(path: String, newpath: String) = {
    Logger.info(s"rename from path $path to $newpath")
    val newname = getName(newpath)
    val oldfile = notebookFile(path)
    Logger.debug(s"rename from path $path to $newpath: old file is ${oldfile.getAbsolutePath}")
    load(path).foreach { notebook =>
      val nb = if (notebook.name != newname) {
        val meta = notebook.metadata.map(_.copy(name = newname))
          .orElse(Some(Metadata(id = Notebook.getNewUUID, name = newname)))
        notebook.copy(metadata = meta)
      } else {
        notebook
      }
      val newfile = notebookFile(newpath)
      Logger.debug(s"rename from path $path to $newpath: new file is ${newfile.getAbsolutePath}")
      oldfile.renameTo(newfile)
      FileUtils.writeStringToFile(newfile, Notebook.serialize(nb))
    }
    (newname, newpath)
  }

  def save(path: String, notebook: Notebook, overwrite: Boolean) = {
    Logger.info(s"save at path $path")
    val file = notebookFile(path)
    if (!overwrite && file.exists()) {
      throw new NotebookExistsException("Notebook " + path + " already exists.")
    }
    FileUtils.writeStringToFile(file, Notebook.serialize(notebook), "UTF-8")
    val nb = load(path)
    (nb.get.metadata.get.name, path)
  }

  def load(path: String): Option[Notebook] = {
    Logger.info(s"Loading notebook at path $path")
    val file = notebookFile(path)
    if (file.exists())
      Notebook.deserialize(FileUtils.readFileToString(file))
    else
      None
  }


}

class NotebookExistsException(message: String) extends IOException(message)
