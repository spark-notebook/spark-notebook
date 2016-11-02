import Dependencies._
import com.typesafe.config.Config
import com.typesafe.sbt.packager.docker.Cmd
import sbt._

import scala.util.Try

object DockerProperties extends BuildConf {

  import scala.collection.JavaConverters._

  val fileName = ".docker.build.conf"

  private val defaultCommands: Seq[Cmd] = Seq(
    Cmd("USER", "root"),
    Cmd("RUN", s"apt-get update --fix-missing && apt-get install -y --no-install-recommends openjdk-7-jdk"),
    Cmd("ENV", "JAVA_HOME /usr/lib/jvm/java-7-openjdk-amd64"),
    Cmd("RUN", s"apt-get install -y wget curl")
  )
  private val defaultVolumes: Seq[String] = Seq("/opt/docker", "/opt/docker/notebooks", "/opt/docker/logs")

  private def asCmdSeq( configs: Seq[Config] ): Seq[Cmd] = {
    configs.flatMap { possibleCmd =>
      val cmd = Try { Some(possibleCmd.getString("cmd")) }.getOrElse(None)
      val arg = Try { Some(possibleCmd.getString("arg")) }.getOrElse(None)
      ( cmd, arg ) match {
        case (Some(c), Some(a)) => Some( Cmd(c,a) )
        case _ => None
      }
    }
  }

  val maintainer   = getString("docker.maintainer", "Andy Petrella")

  val baseImage    = getString("docker.baseImage", "debian:jessie")

  val commands     = Try { asCmdSeq(cfg.getConfigList("docker.commands").asScala.toSeq) }.getOrElse( defaultCommands )
  val volumes      = Try { cfg.getStringList("docker.volumes").asScala.toSeq }.getOrElse( defaultVolumes )
  val registry     = Some(getString("docker.registry", "andypetrella"))
  val ports        = Try { cfg.getIntList("docker.ports").asScala.toSeq.map { e => e.intValue() } }.getOrElse( Seq(9001, 9443) )
}
