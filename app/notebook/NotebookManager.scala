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

  def listNotebooks = {
    val files = notebookDir.listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn =>
      val name = URLDecoder.decode(fn.substring(0, fn.length - extension.length), "UTF-8")
      Json.obj(
        "name" -> name,
        "notebook_id" -> name
      )
    }
    JsArray(res.toList)
  }

  def notebookFile(name: String) = {
    val basePath = notebookDir.getCanonicalPath
    val fileName = URLDecoder.decode(name) + extension//URLEncoder.encode(name, "UTF-8") + extension
    val nbFile = new File(basePath, fileName)
    Logger.info(s"Load notebook. canonical file path: ${nbFile.getCanonicalPath}")
    /* This check is probably not strictly necessary due to URL encoding of name (should escape any path traversal components), but let's be safe */
    require(nbFile.getParentFile.getCanonicalPath == basePath, "Unable to access notebook outside of notebooks path.")
    nbFile
  }

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook() = {
    val name = incrementFileName("Untitled")
    val nb = Notebook(Some(new Metadata(name)), Some(Nil), None, None, None)
    val path = name+extension
    save(name, path, nb, false)
    name
  }

  def copyNotebook(nbName: String, nbPath: String) = {
    val nbData = getNotebook(nbName, nbPath)
    nbData.map { nb =>
    	val name = incrementFileName(nb._2)
    	val oldNB = NBSerializer.read(nb._3)
      val path = name
    	save(name, path, Notebook(Some(new Metadata(name)), oldNB.cells, oldNB.worksheets, oldNB.autosaved, None), false)
    	path
    } getOrElse newNotebook
  }

  /**
   * Attempts to select a notebook by ID first, if supplied and if the ID
   * is known; falls back to supplied name otherwise.
   */
  def getNotebook(name: String, path: String) = {
    val nameToUse = name
    Logger.info(s"getNotebook $name at path $path")
    for (notebook <- load(nameToUse)) yield {
      val data = FileUtils.readFileToString(notebookFile(nameToUse))
      val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'")
      val last_mtime = df.format(new Date(notebookFile(nameToUse).lastModified()))
      (last_mtime, notebook.name, data)
    }
  }

  def deleteNotebook(name: String, path: String) = {
    val nameToUse = name
    val file = notebookFile(nameToUse)
    if (file.exists()) {
      //removeMapping(nameToUse)
      file.delete()
    }
  }


  def rename(name: String, path: String) = {
    val file = notebookFile(name)
    load(name).map { notebook =>
      val nb = if (notebook.name != name) notebook.copy(metadata = Some(new Metadata(name))) else notebook
      file.renameTo(notebookFile(name))
      FileUtils.writeStringToFile(notebookFile(name), NBSerializer.write(nb))

      //setMapping(name, path)
    }
  }

  def save(name: String, path: String, notebook: Notebook, overwrite: Boolean) {
    val file = notebookFile(name)
    if (!overwrite && file.exists()) throw new NotebookExistsException("Notebook " + name + " already exists.")

    val nb = if (notebook.name != name) notebook.copy(metadata = Some(new Metadata(name))) else notebook

    FileUtils.writeStringToFile(notebookFile(name), NBSerializer.write(nb))

    //setMapping(name, path)
  }

  def load(name: String): Option[Notebook] = {
    Logger.info(s"Loading $name")
    val file = notebookFile(name)
    if (file.exists())
      Some(NBSerializer.read(FileUtils.readFileToString(file)))
    else None
  }


}
class NotebookExistsException(message:String) extends IOException(message)
