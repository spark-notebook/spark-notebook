/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

import java.io.{File, FileReader, BufferedReader}

import notebook._, front.widgets._, front.third.d3._
import notebook.util._

import org.json4s.JsonAST._
import org.json4s.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.SparkILoop



@transient var execUri = Option(System.getenv("SPARK_EXECUTOR_URI"))
@transient var execMemory = Option(System.getenv("SPARK_EXECUTOR_MEMORY"))
@transient var sparkHome = Option(System.getenv("SPARK_HOME"))
@transient var sparkMaster = Option(System.getenv("MASTER"))
@transient var jars = SparkILoop.getAddedJars

@transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

@transient var conf = new SparkConf()

@transient var sparkContext:SparkContext = _

def reset(appName:String="Notebook", lastChanges:(SparkConf=>Unit)=(_:SparkConf)=>()):Unit = {
  conf = new SparkConf()
  conf.setMaster(sparkMaster.getOrElse("local[*]"))
      .setAppName(appName)
      .set("spark.repl.class.uri", uri)

  execMemory foreach (v => conf.set("spark.executor.memory", v))
  execUri foreach (v => conf.set("spark.executor.uri", v))
  sparkHome foreach (v => conf.setSparkHome(v))

  conf.setJars(jars)

  lastChanges(conf)

  if (sparkContext != null) sparkContext.stop()
  sparkContext = new SparkContext(conf)
}

reset()

@transient var remotes = List(Repos.central)
@transient var repo:File = _
def updateRepo(dir:String) = {
  val r = new File(dir)
  if (!r.exists) r.mkdirs else ()
  repo = r
  r
}
updateRepo(System.getProperty("java.io.tmpdir")+ s"/scala-notebook/aether/" + java.util.UUID.randomUUID.toString)

def updateJars(newJars:List[String]) = {
  jars = (newJars ::: jars.toList).distinct.toArray
}

def stopSpark() = sparkContext.stop()

@transient implicit val updateSparkContex:SparkContext=>Unit = (sc:SparkContext) => {
  sparkContext = sc
}

"init.sc done!"