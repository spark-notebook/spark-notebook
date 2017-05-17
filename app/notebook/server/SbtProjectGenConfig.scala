package com.datafellas.notebook.adastyx.server

import java.io.File

import com.typesafe.config.Config
import scala.concurrent.duration._
import play.api.{Logger, _}

case class SbtProjectGenConfig(config: Configuration) { me =>

  import play.api.Play.current

  val DefaultTimeout = 90.seconds.toMillis

  val projectsDirConf = config.getString("projects.dir")

  val providerTimeout: Duration = {
    val timeout = config.getMilliseconds("io.provider_timeout").getOrElse(DefaultTimeout)
    new FiniteDuration(timeout, MILLISECONDS)
  }

  // no eager creation of this location
  val projectsDir = projectsDirConf.map(new File(_)).getOrElse {
    throw new RuntimeException("Missing sbt-project-generation.projects.dir configuration key")
  }

  val underlying: Config = config.underlying
  Logger.info(s"Provider timeout is $providerTimeout")
  Logger.info(s"Projects dir is $projectsDir ")
  Logger.info(s"Projects dir exists? ${projectsDir.exists()} ")
  if (projectsDir.exists()) {
    Logger.info(s"Projects content:" + projectsDir.listFiles().map(_.getName).mkString(", "))
  }
}
