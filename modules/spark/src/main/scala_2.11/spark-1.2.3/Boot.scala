package org.apache.spark

import org.apache.spark.util.Utils
import org.apache.spark._

import scala.tools.nsc.Settings

object Boot {
  def classServer = {
    val conf = new SparkConf()

    val tmp = System.getProperty("java.io.tmpdir")
    val rootDir = conf.get("spark.repl.classdir", tmp)
    val outputDir = Utils.createTempDir(rootDir)
    val s = new Settings()
    s.processArguments(List("-Yrepl-class-based", "-Yrepl-outdir", s"${outputDir.getAbsolutePath}", "-Yrepl-sync"), true)
    val server = new HttpServer(conf, outputDir, new SecurityManager(conf))
    server
  }

}