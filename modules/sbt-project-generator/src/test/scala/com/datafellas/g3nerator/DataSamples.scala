package com.datafellas.g3nerator

import java.nio.file.Paths

import com.datafellas.g3nerator.model.DependencyConfig

import scala.io.Source

object DataSamples {

  val CommonUrlConfig = DependencyConfig(
    "resolverCloudera",
    "http://dl.bintray.com/spark-packages/maven/", //resolverBintrayDataFellasMvn",
    "http://dl.bintray.com/spark-packages/maven/",
    Paths.get("/opt/java/jdk"),
    Paths.get("/opt/sbt/"),
    "dockerBaseImage",
    "https://bintray.com/sbt/sbt-plugin-releases"
  )

  def loadNotebook(name:String): String = {
    val sampleNotebookDir = "/snb/"
    Source.fromURL(getClass.getResource(sampleNotebookDir + name)).getLines().mkString("\n")
  }


}
