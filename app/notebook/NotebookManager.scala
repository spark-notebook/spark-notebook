package notebook
package server

import java.io._
import java.net.{URLEncoder, URLDecoder}
import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import org.apache.commons.io.FileUtils

import play.api.Logger
import play.api.libs.json._

import notebook.NBSerializer._

class NotebookManager(val name: String, val notebookDir: File) {

  Logger.info("Notebook directory is: " + notebookDir.getCanonicalPath)

  val extension = ".snb"

  def getName(path:String) = path.split("/").filter(!_.isEmpty).last.dropRight(extension.size)

  def notebookFile(path: String) = {
    Logger.info(s"Load notebook. initial path: ${path}")
    val basePath = notebookDir.getCanonicalPath
    Logger.info(s"Load notebook. base canonical file path: ${basePath}")
    val decodedPath = URLDecoder.decode(path)
    Logger.info(s"Load notebook. decodedPath: ${decodedPath}")
    val nbFile = new File(basePath, decodedPath)
    Logger.info(s"Load notebook. canonical file path: ${nbFile.getCanonicalPath}")
    Logger.info(s"Load notebook. absolute file path: ${nbFile.getAbsolutePath}")
    /* This check is probably not strictly necessary due to URL encoding of name (should escape any path traversal components), but let's be safe */
    require(nbFile.getCanonicalPath.startsWith(basePath), "Unable to access notebook outside of notebooks path.")
    nbFile
  }

  def incrementFileName(base:String) = {
    Logger.info("Incremented Notebook at " + base)
    val newPath:String = Stream.from(1).map { i => base + i + extension }.filterNot { fn => notebookFile(fn).exists() }.head
    Logger.info("Incremented Notebook is " + newPath)
    newPath
  }

  def newNotebook(
    path:String = "/",
    customLocalRepo:Option[String]=None,
    customRepos:Option[List[String]]=None,
    customDeps:Option[List[String]]=None,
    customImports:Option[List[String]]=None,
    customSparkConf:Option[JsObject]=None) = {
    val fpath = incrementFileName(path+(if (path.last=='/')""else"/")+"Untitled")
    val nb =  Notebook(
                Some(new Metadata(getName(fpath),
                                  customLocalRepo=customLocalRepo,
                                  customRepos=customRepos,
                                  customDeps=customDeps,
                                  customImports=customImports,
                                  customSparkConf=customSparkConf)),
                Some(Nil),
                None,
                None,
                None
              )
    save(fpath, nb, false)
    fpath
  }

  def copyNotebook(nbPath: String) = {
    val nbData = getNotebook(nbPath)
    nbData.map { nb =>
    	val newPath = incrementFileName(nb._4.dropRight(extension.size))
      val newName = getName(newPath)
    	val oldNB = NBSerializer.read(nb._3)
    	save(newPath, Notebook(oldNB.metadata.map(_.copy(name = newName)), oldNB.cells, oldNB.worksheets, oldNB.autosaved, None), false)
    	newPath
    } getOrElse newNotebook()
  }

  def getNotebook(path: String) = {
    Logger.info(s"getNotebook at path $path")
    for (notebook <- load(path)) yield {
      val data = FileUtils.readFileToString(notebookFile(path))
      val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'")
      val last_mtime = df.format(new Date(notebookFile(path).lastModified()))
      (last_mtime, notebook.name, data, path)
    }
  }

  def deleteNotebook(path: String) = {
    val file = notebookFile(path)
    if (file.exists()) {
      file.delete()
    }
  }


  def rename(path: String, newpath: String) = {
    val newname = getName(newpath)
    val oldfile = notebookFile(path)
    load(path).map { notebook =>
      val nb =  if (notebook.name != newname) {
                  val newMd =  notebook.metadata.map(_.copy(name=newname))
                                                .orElse(Some(new Metadata(newname)))
                  notebook.copy(metadata = newMd)
                } else {
                  notebook
                }
      //val nb = if (notebook.name != newname) notebook.copy(metadata = Some(new Metadata(newname))) else notebook
      val newfile = notebookFile(newpath)
      oldfile.renameTo(newfile)
      FileUtils.writeStringToFile(newfile, NBSerializer.write(nb))
    }
    (newname, newpath)
  }

  def save(path: String, notebook: Notebook, overwrite: Boolean) = {
    val file = notebookFile(path)
    if (!overwrite && file.exists()) throw new NotebookExistsException("Notebook " + path + " already exists.")
    FileUtils.writeStringToFile(file, NBSerializer.write(notebook))
    val nb = load(path)
    (nb.get.metadata.get.name, path)
  }

  def load(path: String): Option[Notebook] = {
    Logger.info(s"Loading $path")
    val file = notebookFile(path)
    if (file.exists())
      Some(NBSerializer.read(FileUtils.readFileToString(file)))
    else None
  }


}
class NotebookExistsException(message:String) extends IOException(message)