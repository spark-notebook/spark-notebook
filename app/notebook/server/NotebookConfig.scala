package notebook.server

import java.io.{File, InputStream}
import java.net.URL

import notebook.Notebook
import notebook.io.{NotebookProvider, NotebookProviderFactory}
import notebook.util.StringUtils
import org.apache.commons.io.FileUtils
import play.api.libs.json._
import play.api.{Logger, _}
import utils.ConfigurationUtils._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.control.Exception.allCatch

case class NotebookConfig(config: Configuration) {
  me =>

  import play.api.Play.current

  config.getString("notebooks.dir").foreach { confDir =>
    Logger.debug(s"Notebooks directory in the config is referring $confDir. Does it exist? ${new File(confDir).exists}")
  }

  val viewer:Boolean = config.getBoolean("viewer_mode").getOrElse(false)

  val customConf = CustomConf.fromConfig(config.getConfig("notebooks.custom"))

  val overrideConf = CustomConf.fromConfig(config.getConfig("notebooks.override"))

  val projectName = config.getString("name").getOrElse(notebooksDir.getPath)

  val defaultIoProviderTimeout = 60.seconds.toMillis
  val ioProviderTimeout: Long = {
    val timeout = config.getMilliseconds("notebooks.io.provider_timeout").getOrElse(defaultIoProviderTimeout)
    Logger.info(s"io.provider_timeout: $timeout ms")
    timeout
  }
  val notebookIoProviderClass = config.tryGetString("notebooks.io.provider").get
  val notebookIoProvider: NotebookProvider = {
      val configuration = config.tryGetConfig(notebookIoProviderClass).get
      NotebookProviderFactory.createNotebookIoProvider(notebookIoProviderClass, configuration.underlying, ioProviderTimeout)
  }

  val notebooksDir = notebookIoProvider.root.toFile
  Logger.info(s"Notebooks dir is $notebooksDir")

  val serverResources = config.getStringList("resources").map(_.asScala).getOrElse(Nil).map(new File(_))

  val maxBytesInFlight = config.underlying.getBytes("maxBytesInFlight").toInt

  object kernel {
    val config = me.config.getConfig("kernel").getOrElse(Configuration.empty)
    val defauldInitScript = config.getString("default.init")
                                  .orElse(Some("init.sc"))
                                  .flatMap { init =>
                                    current.resource("scripts/" + init).map(i => ScriptFromURL(i).toSource)
                                  }
    val kernelInit = {
      val scripts = config.getStringList("init").map(_.asScala).getOrElse(Nil).map(
        url => ScriptFromURL(new URL(url)))
      defauldInitScript.map { s => s :: scripts.toList }.getOrElse(scripts)
    }
    val initScripts = kernelInit.map(x => (x.name, x.script))
    val compilerArgs = config.getStringList("compilerArgs").map(_.asScala).getOrElse(Nil)
    val vmConfig = config.underlying
  }
}

case class CustomConf(
  localRepo: Option[String],
  repos:     Option[List[String]],
  deps:      Option[List[String]],
  imports:   Option[List[String]],
  args:      Option[List[String]],
  sparkConf: Option[JsObject]
) {
  val sparkConfMap:Option[Map[String, String]] = sparkConf flatMap CustomConf.fromSparkConfJsonToMap
}
object CustomConf {
  def fromConfig(custom:Option[Configuration]) = {
    val localRepo = custom.flatMap(_.getString("localRepo"))
    val repos = custom.flatMap(_.getStringList("repos")).map(_.asScala.toList)
    val deps = custom.flatMap(_.getStringList("deps")).map(_.asScala.toList)
    val imports = custom.flatMap(_.getStringList("imports")).map(_.asScala.toList)
    val args = custom.flatMap(_.getStringList("args")).map(_.asScala.toList)
    val sparkConf = custom.flatMap(_.getConfig("sparkConf")).map { c =>
      JsObject(c.entrySet.map { case (k, v) => (k, JsString(v.unwrapped().toString)) }.toSeq)
    }
    CustomConf(localRepo, repos, deps, imports, args, sparkConf)
  }

  private val readJsValueMap = Reads.map[JsValue]
  def fromSparkConfJsonToMap(conf:JsObject):Option[Map[String, String]] =
    for {
      map <- readJsValueMap.reads(conf).asOpt
    } yield map.map {
      case (k, a@JsArray(v))   => k → a.toString
      case (k, JsBoolean(v))   => k → v.toString
      case (k, JsNull)         => k → "null"
      case (k, JsNumber(v))    => k → v.toString
      case (k, o@JsObject(v))  => k → o.toString
      case (k, JsString(v))    => k → v
    }

}

trait Script {
  def name: String

  def script: String
}

case class ScriptFromURL(url: URL) extends Script {
  val name = url.toExternalForm

  def script = {
    var is: InputStream = null
    allCatch.andFinally(if (is != null) is.close()).either {
      is = url.openStream()
      scala.io.Source.fromInputStream(is).getLines().mkString("\n")
    } match {
      case Right(s) => s
      case Left(e) => Logger.warn("Unable to read initscript from %s".format(url), e); ""
    }
  }

  def toSource = ScriptFromSource(url.toExternalForm, script)
}

case class ScriptFromFile(file: File) extends Script {
  val name = file.getAbsolutePath

  def script = {
    allCatch.either(FileUtils.readFileToString(file)) match {
      case Right(s) => s
      case Left(e) => Logger.warn("Unable to read initscript from %s".format(file), e); ""
    }
  }
}

case class ScriptFromSource(name: String, script: String) extends Script
