/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

import com.bwater.notebook._, widgets._
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.SparkILoop

@transient val execUri = System.getenv("SPARK_EXECUTOR_URI")
@transient val sparkMaster = System.getenv("SPARK_MASTER")
@transient val jars = SparkILoop.getAddedJars
@transient val conf = new SparkConf()
  .setMaster(Option(sparkMaster).getOrElse("local[*]"))
  .setAppName("Notebook")
  .setJars(jars)
  //.set("spark.repl.class.uri", intp.classServer.uri)
if (execUri != null) {
  conf.set("spark.executor.uri", execUri)
}
if (System.getenv("SPARK_HOME") != null) {
  conf.setSparkHome(System.getenv("SPARK_HOME"))
}
@transient val sparkContext = new SparkContext(conf)
