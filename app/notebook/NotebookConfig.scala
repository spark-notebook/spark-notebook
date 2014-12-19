package notebook.server

import java.io.{File, InputStream}
import java.net.URL

import scala.collection.JavaConverters._
import scala.util.control.Exception.allCatch

import org.apache.commons.io.FileUtils

import com.typesafe.config._

import play.api._
import play.api.Logger

case class NotebookConfig(config: Configuration) {
  val defauldInitScript = config.getString("kerner.default.init").map { init =>
                            ScriptFromURL(classOf[NotebookConfig].getResource(init)).toSource
                          }

  val kernelInit = {
    val scripts = config.getStringList("kernel.init").map(_.asScala).getOrElse(Nil).map(url => ScriptFromURL(new URL(url)))
    defauldInitScript.map { s => s :: scripts.toList }.getOrElse(scripts)
  }
  val kernelInitScripts = kernelInit.map(_.script)

  val serverResources = config.getStringList("resources").map(_.asScala).getOrElse(Nil).map(new File(_))

  val kernelCompilerArgs = config.getStringList("compilerArgs").map(_.asScala).getOrElse(Nil)

  val notebooksDir = config.getString("notebooks.dir").map(new File(_)).getOrElse(new File("."))

  val projectName = config.getString("notebooks.name").getOrElse(notebooksDir.getPath())

  val kernelVMConfig = config.underlying
}


trait Script {
  def script: String;
}
case class ScriptFromURL(url: URL) extends Script {
  def script = {
    var is: InputStream = null
    allCatch.andFinally(if (is != null) is.close()).either {
      is = url.openStream();
      scala.io.Source.fromInputStream(is).getLines().mkString("\n")
    } match {
      case Right(s) => s
      case Left(e) => Logger.warn("Unable to read initscript from %s".format(url), e); ""
    }
  }

  def toSource = ScriptFromSource(script)
}
case class ScriptFromFile(file: File) extends Script {
  def script = {
    allCatch.either(FileUtils.readFileToString(file)) match {
      case Right(s) => s
      case Left(e) => Logger.warn("Unable to read initscript from %s".format(file), e); ""
    }
  }
}
case class ScriptFromSource(script: String) extends Script
