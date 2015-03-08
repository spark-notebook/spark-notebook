import java.io.{File, FileReader, BufferedReader}

import notebook._, front._, widgets._, third.d3._
import notebook.JsonCodec._
import notebook.util._


import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._


@transient var execUri = Option(System.getenv("SPARK_EXECUTOR_URI"))
@transient var execMemory = Option(System.getenv("SPARK_EXECUTOR_MEMORY"))
@transient var sparkHome = Option(System.getenv("SPARK_HOME"))
@transient var sparkMaster = Option(System.getenv("MASTER"))

@transient val addedJars: Array[String] = {    val envJars = sys.env.get("ADD_JARS")
  val propJars = sys.props.get("spark.jars").flatMap { p => if (p == "") None else Some(p) }
  val jars = propJars.orElse(envJars).getOrElse("")
  notebook.Utils.resolveURIs(jars).split(",").filter(_.nonEmpty)
}

@transient var jars = (addedJars ++ CustomJars).distinct

@transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

@transient var conf = new SparkConf().setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)

@transient var sparkContext:SparkContext = _

@transient val SparkNotebookBgLog = ul(20)

def reset(appName:String="Notebook", lastChanges:(SparkConf=>Unit)=(_:SparkConf)=>()):Unit = {
  SparkNotebookBgLog.append("Calling reset")
  conf = new SparkConf()
  conf.setMaster(sparkMaster.getOrElse("local[*]"))
      .setAppName(appName)
      .set("spark.repl.class.uri", uri)
      .setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)

  execMemory foreach (v => conf.set("spark.executor.memory", v))
  execUri foreach (v => conf.set("spark.executor.uri", v))
  sparkHome foreach (v => conf.setSparkHome(v))

  conf.setJars(jars)

  lastChanges(conf)

  if (sparkContext != null) {
    SparkNotebookBgLog.append("Stopping Spark Context")
    sparkContext.stop()
    SparkNotebookBgLog.append("Spark Context stopped")
  }
  SparkNotebookBgLog.append("Starting Spark Context")
  sparkContext = new SparkContext(conf)
  SparkNotebookBgLog.append("Stopping Spark Context")
}

reset()

def updateJars(newJars:List[String]) = {
  jars = (newJars ::: jars.toList).distinct.toArray
}

def stopSpark() = sparkContext.stop()

@transient implicit val updateSparkContex:SparkContext=>Unit = (sc:SparkContext) => {
  sparkContext = sc
}

SparkNotebookBgLog.append("Initialized!")

"init.sc done!"
