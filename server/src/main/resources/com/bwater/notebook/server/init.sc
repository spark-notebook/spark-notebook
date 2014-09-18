/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

import java.io.{FileReader, BufferedReader}

import com.bwater.notebook._, widgets._, d3._
import com.bwater.notebook.client.SparkClassServerUri
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.SparkILoop


@transient var execUri = System.getenv("SPARK_EXECUTOR_URI")
@transient var sparkHome = System.getenv("SPARK_HOME")
@transient var sparkMaster = System.getenv("MASTER")
@transient var jars = SparkILoop.getAddedJars

@transient val uri = _5C4L4_N0T3800K_5P4RK_HOOK

@transient var conf = new SparkConf()

def reset(assign:Boolean=true):SparkContext = {
  conf = new SparkConf()
  conf.setMaster(Option(sparkMaster).getOrElse("local[*]"))
      .setAppName("Notebook")
      .set("spark.repl.class.uri", uri)
  if (execUri != null) {
    conf.set("spark.executor.uri", execUri)
  }
  if (sparkHome != null) {
    conf.setSparkHome(sparkHome)
  }
  conf.setJars(jars)
  val sc = new SparkContext(conf)
  if (assign) sparkContext = sc
  sc
}

@transient var sparkContext:SparkContext = reset(false)
