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

  val extension = ".snb"

  def listNotebooks = {
    val files = notebookDir.listFiles map {_.getName} filter {_.endsWith(extension)} toIndexedSeq
    val res = files.sorted map { fn =>
      val name = URLDecoder.decode(fn.substring(0, fn.length - extension.length), "UTF-8")
      Json.obj(
        "name" -> name,
        "notebook_id" -> notebookId(name)
      )
    }
    JsArray(res.toList)
  }

  def notebookFile(name: String) = {
    Logger.info(s"Load notebook: $name")
    val basePath = notebookDir.getCanonicalPath
    Logger.info(s"Load notebook. basePath: $basePath")
    val fileName = name + extension//URLEncoder.encode(name, "UTF-8") + extension
    Logger.info(s"Load notebook. file name: $fileName")
    val nbFile = new File(basePath, fileName)
    Logger.info(s"Load notebook. canonical file path: ${nbFile.getParentFile.getCanonicalPath}")
    /* This check is probably not strictly necessary due to URL encoding of name (should escape any path traversal components), but let's be safe */
    require(nbFile.getParentFile.getCanonicalPath == basePath, "Unable to access notebook outside of notebooks path.")
    nbFile
  }

  def incrementFileName(base:String) = {
    Stream.from(1) map { i => base + i } filterNot { fn => notebookFile(fn).exists() } head
  }

  def newNotebook() = {
    val name = incrementFileName("Untitled")
    val nb = Notebook(new Metadata(name), Nil, None, None, None)
    val id = notebookId(name)
    save(Some(id), name, nb, false)
    id
  }

  def copyNotebook(nbId: Option[String], nbName: String) = {
    val nbData = getNotebook(nbId, nbName)
    nbData.map { nb =>
    	val name = incrementFileName(nb._2)
    	val oldNB = NBSerializer.read(nb._3)
    	val id = notebookId(name)
    	save(Some(id), name, Notebook(new Metadata(name), oldNB.cells, oldNB.worksheets, oldNB.autosaved, None), false)
    	id
    } getOrElse newNotebook
  }

  /**
   * Attempts to select a notebook by ID first, if supplied and if the ID
   * is known; falls back to supplied name otherwise.
   */
  def getNotebook(id: Option[String], name: String) = {
    val nameToUse = id flatMap idToName.get getOrElse name
    for (notebook <- load(nameToUse)) yield {
      val data = FileUtils.readFileToString(notebookFile(notebook.name))
      val df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z'('Z')'")
      val last_mtime = df.format(new Date(notebookFile(notebook.name).lastModified()))
      (last_mtime, notebook.name, data)
    }
  }

  def deleteNotebook(id: Option[String], name: String) = {
    val realId = id match {
      case Some(x) => id
      case None => nameToId(name)
    }
    val nameToUse = realId flatMap idToName.get getOrElse name
    val file = notebookFile(nameToUse)
    if (file.exists()) {
      realId foreach removeMapping
      file.delete()
    }
  }


  def save(id: Option[String], name: String, nbI: Notebook, overwrite: Boolean) {
    val file = notebookFile(name)
    if (!overwrite && file.exists()) throw new NotebookExistsException("Notebook " + name + " already exists.")

    val nb = if (nbI.name != name) nbI.copy(new Metadata(name)) else nbI
    FileUtils.writeStringToFile(notebookFile(name), NBSerializer.write(nb))
    // If there was an old file that's different, then delete it because this is a rename
    id flatMap idToName.get foreach { oldName =>
      if (notebookFile(nb.name).compareTo(notebookFile(oldName)) != 0)
        notebookFile(oldName).delete()
    }

    setMapping(id getOrElse notebookId(name), name)
  }

  def load(name: String): Option[Notebook] = {
    val file = notebookFile(name)
    if (file.exists())
      Some(NBSerializer.read(FileUtils.readFileToString(file)))
    else None
  }


  private def removeMapping(id: String) {
    idToName.remove(id)
  }
  private def setMapping(id: String, name:String) {
    nameToId(name).foreach(idToName.remove(_))
    idToName.put(id, name)
  }

  val idToName = collection.mutable.Map[String, String]()
  def nameToId(name: String) = idToName.find(_._2 == name).map(_._1)

  def notebookId(name: String) = nameToId(name) getOrElse {
    val id = UUID.randomUUID.toString
    setMapping(id, name)
    id
  }

}
class NotebookExistsException(message:String) extends IOException(message)
