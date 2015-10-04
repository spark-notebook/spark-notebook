import java.io.{File, FileReader, BufferedReader}

import notebook._
import notebook.front._
import notebook.front.widgets._
import notebook.front.third.d3._
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.JsonCodec._


import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.SparkContext._
import org.apache.spark.rdd._

@transient val globalScope = new java.io.Serializable {
  @transient var execUri = Option(System.getenv("SPARK_EXECUTOR_URI"))
  @transient var execMemory = Option(System.getenv("SPARK_EXECUTOR_MEMORY"))
  @transient var sparkHome = Option(System.getenv("SPARK_HOME"))
  @transient var sparkMaster = Option(System.getenv("MASTER"))


  @transient val addedJars: Array[String] = {
    val envJars = sys.env.get("ADD_JARS")
    val propJars = sys.props.get("spark.jars").flatMap { p => if (p == "") None else Some(p) }
    val jars = List(propJars, envJars).collect{case Some(j) => j}.mkString(",")
    notebook.Utils.resolveURIs(jars).split(",").filter(_.nonEmpty)
  }


  @transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

  @transient var conf = new SparkConf().setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)

  @transient var jars = (addedJars ++ CustomJars ++ conf.get("spark.jars", ",").split(",")).distinct

  @transient var sparkContext:SparkContext = _

  def reset(appName:String=notebookName, lastChanges:(SparkConf=>Unit)=(_:SparkConf)=>()):Unit = {
    conf = new SparkConf()
    conf.setMaster(sparkMaster.getOrElse("local[*]"))
        .setAppName(appName)
        .setAll(_5C4L4_N0T3800K_5P4RK_C0NF.toList)
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
    sparkContext.hadoopConfiguration.set("fs.tachyon.impl", "tachyon.hadoop.TFS")
  }


  val stopSpark:()=>Unit = () => sparkContext.stop()

  def sc:SparkContext = sparkContext
}

import globalScope.{sparkContext, reset, sc, stopSpark}

reset()

println("init.sc done!")
()