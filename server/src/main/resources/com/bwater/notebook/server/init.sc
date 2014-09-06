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

val execUri = System.getenv("SPARK_EXECUTOR_URI")
val jars = SparkILoop.getAddedJars
val conf = new SparkConf()
  .setMaster("local[*]")
  .setAppName("Spark shell")
  .setJars(jars)
  //.set("spark.repl.class.uri", intp.classServer.uri)
if (execUri != null) {
  conf.set("spark.executor.uri", execUri)
}
if (System.getenv("SPARK_HOME") != null) {
  conf.setSparkHome(System.getenv("SPARK_HOME"))
}
@transient val sparkContext = new SparkContext(conf)
