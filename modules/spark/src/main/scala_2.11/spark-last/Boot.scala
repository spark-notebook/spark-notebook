package org.apache.spark

import org.apache.spark.util.Utils
import org.apache.spark._
import scala.tools.nsc.Settings
import java.io.File

object Boot {
  /*def classServer(outputDir:File) = {
    val conf = new SparkConf()
    val server = new HttpServer(conf, outputDir, new SecurityManager(conf))
    server
  }*/

  def createTempDir(
      root: String = System.getProperty("java.io.tmpdir"),
      namePrefix: String = "spark"): File = Utils.createTempDir(root, namePrefix)

}