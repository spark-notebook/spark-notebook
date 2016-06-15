import java.io.{File, FileReader, BufferedReader}
import java.net.URI

import notebook._
import notebook.front._
import notebook.front.widgets._
import notebook.front.widgets.charts._
import notebook.front.third.d3._
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.JsonCodec._


import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

import scala.util.matching.Regex

@transient val globalScope = new java.io.Serializable {
  @transient var execUri = Option(System.getenv("SPARK_EXECUTOR_URI"))
  @transient var execMemory = Option(System.getenv("SPARK_EXECUTOR_MEMORY"))
  @transient var sparkHome = Option(System.getenv("SPARK_HOME"))
  @transient var sparkMaster = Option(System.getenv("MASTER"))

  /* -------------------  URI Helpers -------------------------- */
  /**
    * Whether the underlying operating system is Windows.
    */
  @transient val isWindows = sys.props.getOrElse("os.name", "").startsWith("Windows")

  /**
    * Whether the underlying operating system is Mac OS X.
    */
  @transient val isMac = sys.props.getOrElse("os.name", "").startsWith("Mac OS X")

  /**
    * Pattern for matching a Windows drive, which contains only a single alphabet character.
    */
  @transient val windowsDrive = "([a-zA-Z])".r

  /**
    * Format a Windows path such that it can be safely passed to a URI.
    */
  private def formatWindowsPath(path: String): String = path.replace("\\", "/")

  /**
    * Indicates whether Spark is currently running unit tests.
    */
  private def isTesting = sys.env.contains("SPARK_TESTING") || sys.props.contains("spark.testing")

  /**
    * Return a well-formed URI for the file described by a user input string.
    *
    * If the supplied path does not contain a scheme, or is a relative path, it will be
    * converted into an absolute path with a file:// scheme.
    */
  private def resolveURI(path: String, testWindows: Boolean = false): URI = {

    // In Windows, the file separator is a backslash, but this is inconsistent with the URI format
    val windows = isWindows || testWindows
    val formattedPath = if (windows) formatWindowsPath(path) else path

    val uri = new URI(formattedPath)
    if (uri.getPath == null) {
      throw new IllegalArgumentException(s"Given path is malformed: $uri")
    }

    Option(uri.getScheme) match {
      case Some(windowsDrive(d)) if windows =>
        new URI("file:/" + uri.toString.stripPrefix("/"))
      case None =>
        // Preserve fragments for HDFS file name substitution (denoted by "#")
        // For instance, in "abc.py#xyz.py", "xyz.py" is the name observed by the application
        val fragment = uri.getFragment
        val part = new File(uri.getPath).toURI
        new URI(part.getScheme, part.getPath, fragment)
      case Some(other) =>
        uri
    }
  }

  /** Resolve a comma-separated list of paths. */
  private def resolveURIs(paths: String, testWindows: Boolean = false): String = {
    if (paths == null || paths.trim.isEmpty) {
      ""
    } else {
      paths.split(",").map(_.trim).filter(!_.isEmpty).map { p => resolveURI(p, testWindows) }.mkString(",")
    }
  }

  /* --------------------- end of URI Helpers ---------- */


  @transient val addedJars: Array[String] = {
    val envJars = sys.env.get("ADD_JARS")
    val propJars = sys.props.get("spark.jars").flatMap { p => if (p == "") None else Some(p) }
    val jars = List(propJars, envJars).collect{case Some(j) => j}.mkString(",")
    resolveURIs(jars).split(",").filter(_.nonEmpty)
  }


  @transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

  @transient var conf = new SparkConf().setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)

  @transient var jars = (addedJars ++ CustomJars ++ conf.get("spark.jars", ",").split(",")).distinct

  @transient var sparkContext:SparkContext = _

  import org.apache.spark.ui.notebook.front.gadgets.SparkMonitor
  @transient var sparkMonitor:Option[SparkMonitor] = _

  def reset(appName:String=notebookName, lastChanges:(SparkConf=>Unit)=(_:SparkConf)=>()):Unit = {
    conf = new SparkConf()
    conf.setMaster(sparkMaster.getOrElse("local[*]"))
      .setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)
      .setAppName(appName)
      .set("spark.repl.class.uri", uri)

    execMemory foreach (v => conf.set("spark.executor.memory", v))
    execUri foreach (v => conf.set("spark.executor.uri", v))
    sparkHome foreach (v => conf.setSparkHome(v))

    conf.setJars(jars)

    lastChanges(conf)

    if (sparkContext != null) {
      sparkContext.stop()
    }
    sparkContext = new SparkContext(conf)
    sparkMonitor = Some(new SparkMonitor(sparkContext))
    sparkMonitor.get.start
  }

  def sc:SparkContext = sparkContext
}

import globalScope.{sparkContext, reset, sc}

reset()

println("init.sc done!")
()