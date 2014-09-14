/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

import java.io.{FileReader, BufferedReader}

import com.bwater.notebook._, widgets._
import com.bwater.notebook.client.SparkClassServerUri
import net.liftweb.json.JsonAST.JArray
import net.liftweb.json.JsonDSL._

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.repl.SparkILoop

import scala.concurrent.Await


@transient val sparkContext = {
  val execUri = System.getenv("SPARK_EXECUTOR_URI")
  val sparkMaster = System.getenv("MASTER")
  val jars = SparkILoop.getAddedJars

  val f = new java.io.File("/tmp/very-hackish-spark-classserver-uri")
  val b = new BufferedReader(new FileReader(f))
  val uri = b.readLine()
  b.close()

  val conf = new SparkConf()
    .setMaster(Option(sparkMaster).getOrElse("local[*]"))
    .setAppName("Notebook")
    .setJars(jars)
    .set("spark.repl.class.uri", uri)
  if (execUri != null) {
    conf.set("spark.executor.uri", execUri)
  }
  if (System.getenv("SPARK_HOME") != null) {
    conf.setSparkHome(System.getenv("SPARK_HOME"))
  }
  new SparkContext(conf)
}
