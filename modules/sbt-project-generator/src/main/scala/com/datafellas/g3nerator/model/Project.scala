package com.datafellas.g3nerator.model

import java.io.File
import java.net.{URL => JURL}
import java.nio.file.{Files, Path, Paths}

import com.datafellas.g3nerator.Job
import com.typesafe.config.Config
import notebook.Notebook

import scala.util.Try

class Project(
               val snb: Notebook,
               projectDir: File,
               initialConfig: ProjectConfig,
               val dependencyConfig: DependencyConfig,
               val productionConfig: Option[ProductionConfig] = None
             ) {

  val RepoPrefix = "repo_gen"
  val repo = Files.createTempDirectory(s"${RepoPrefix}_${config.pkg}_${config.version}").toFile
  val name = snb.normalizedName
  val targetDir = new File (projectDir, name)
  def config = initialConfig.copy(outputDirectory = Some(name), name = Some(name))

  def mkJob: Job = new Job(this, repo, targetDir)

}

case class ProductionConfig(sparkVersion: String, hadoopVersion:String)

case class ProjectConfig(
                          pkg:String,
                          version:String,
                          maintainer:String,
                          dockerRepo:String,
                          dockercfg:String,
                          mesosVersion:String,
                          name: Option[String] = None,
                          outputDirectory: Option[String] = None
                        ) {

  def toMap: Map[String, String] = {
    val fixedConfig = Map("pkg" -> pkg,
      "version" -> version,
      "maintainer" -> maintainer,
      "dockerRepo" -> dockerRepo,
      "dockercfg" -> dockercfg,
      "mesosVersion" -> mesosVersion
    )
    val optionalConfig = List(name.map("name" -> _), outputDirectory.map("outputDirectory" -> _)).flatten
    fixedConfig ++ optionalConfig
  }
}