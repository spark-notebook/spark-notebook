package com.bwater.notebook.server

import java.io._
import collection.JavaConversions._
import com.bwater.notebook.util.Logging
import com.typesafe.config._
import com.bwater.notebook.kernel.ConfigUtils._
import com.bwater.notebook.Server
import org.apache.commons.io.FileUtils
import java.net.URL
import com.bwater.notebook.kernel.pfork.ProcessFork
import com.bwater.notebook.Server
import scala.collection.mutable.MapBuilder
import util.control.Exception.allCatch

/**
 * Configuration for scala notebook server, kernel, and kernel VM.
 *
 * Mostly exists for merge behavior in kernelInit and serverResources.
 */
case class ScalaNotebookConfig(kernelInit: List[Script], serverResources: List[File], kernelCompilerArgs: List[String], kernelVMConfig: Config) {
  def notebooksDir = kernelVMConfig.get("notebooks.dir").map(new File(_)).getOrElse(new File("."))
  
  def projectName = kernelVMConfig.get("notebooks.name").getOrElse(notebooksDir.getPath())

  def kernelInitScripts: List[String] = kernelInit.map(_.script)

  def ++(other: ScalaNotebookConfig) =
    new ScalaNotebookConfig(
      other.kernelInit ++ kernelInit, 
      serverResources ++ other.serverResources, 
      kernelCompilerArgs ++ other.kernelCompilerArgs, 
      kernelVMConfig.withFallback(other.kernelVMConfig).resolve()
    )
}

object ScalaNotebookConfig extends Logging {

  
  def apply(cfg: Config) = {
    val config = cfg.resolve()
    val kInit = config.getArray("kernel.init").getOrElse(Nil).map(url => ScriptFromURL(new URL(url)))
    val resources = config.getArray("resources").getOrElse(Nil).map(new File(_))
    val cArgs = config.getArray("compilerArgs").getOrElse(Nil)
    new ScalaNotebookConfig(kInit, resources, cArgs, config)
  }

  def apply(mappings: (String, Any)*): ScalaNotebookConfig =
    apply(ConfigFactory.parseMap(javaficate(mappings.toMap).asInstanceOf[java.util.Map[String, Any]]))
    
  
  lazy val defaults = new ScalaNotebookConfig(snKernelInit :: Nil, Nil, Nil, ConfigFactory.empty)

  def propertyOverrides = {
    val defaultCfg = ConfigFactory.defaultOverrides()
    val overrides = if (defaultCfg.hasPath("notebook")) allCatch.opt(defaultCfg.getConfig("notebook")).getOrElse(ConfigFactory.empty) 
                    else ConfigFactory.empty
    apply(overrides)
  }
  
  /**
   * Augments the specified config: [default overrides] ++ [user overrides] ++ base
   */
  def withOverrides(base: ScalaNotebookConfig) = {
    val propOverrides = ScalaNotebookConfig.propertyOverrides;
    propOverrides ++ userOverrides((propOverrides ++ base).notebooksDir) ++ base
  }

  /*
   * User configuration overrides in the notebook directory (if present).
   *
   * TODO: these are processed on the server, the kernel vm should have its own overrides based on where it is executing.
   */
  def userOverrides(location: File) = {
    val file = new File(location, ".conf")
    val hasInit = file.exists()
    val config = if (hasInit) {
      val reader = new BufferedReader(new FileReader(file))
      try ConfigFactory.parseReader(reader, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.CONF))
      finally {
        reader.close()
      }
    } else {
      logWarn("User configuration file %s was not found, using defaults.".format(file))
      ConfigFactory.empty()
    }
    val sConfig = ScalaNotebookConfig(config.resolve())
    if (hasInit)
      sConfig.copy(kernelInit = sConfig.kernelInit :+ ScriptFromFile(new File(location, "init.scala")))
    else
      sConfig
  }


  /* URL for builtin kernel initialization script */
  lazy val snKernelInit = ScriptFromURL(classOf[ScalaNotebookConfig].getResource("init.sc")).toSource

  private val javaficate: Any => Any = {
    case m: Map[_, _] => mapAsJavaMap(m.mapValues(javaficate))
    case i: Iterable[_] => asJavaIterable(i.map(javaficate))
    case x => x
  }
}

trait Script {
  def script: String;
}
case class ScriptFromURL(url: URL) extends Script with Logging {
  def script = {
    var is: InputStream = null
    allCatch.andFinally(if (is != null) is.close()).either {
      is = url.openStream();
      scala.io.Source.fromInputStream(is).getLines().mkString("\n")
    } match {
      case Right(s) => s
      case Left(e) => logWarn("Unable to read initscript from %s".format(url), e); ""
    }
  }

  def toSource = ScriptFromSource(script)
}
case class ScriptFromFile(file: File) extends Script with Logging {
  def script = {
    allCatch.either(FileUtils.readFileToString(file)) match {
      case Right(s) => s
      case Left(e) => logWarn("Unable to read initscript from %s".format(file), e); ""
    }
  }
}
case class ScriptFromSource(script: String) extends Script
