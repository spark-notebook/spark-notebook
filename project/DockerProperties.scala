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
    Cmd("RUN", "apt-key adv --keyserver keyserver.ubuntu.com --recv E56151BF"),
    Cmd("RUN", "echo \"deb http://repos.mesosphere.io/ubuntu trusty main\" | tee /etc/apt/sources.list.d/mesosphere.list"),
    Cmd("RUN", "/usr/bin/apt-get -y update --fix-missing"),
    Cmd("RUN", s"/usr/bin/apt-get -y install mesos=$mesosVersion-1.0.ubuntu1404"), //ubuntu 14.04 is base for java:latest → https://github.com/dockerfile/ubuntu/blob/master/Dockerfile
    Cmd("ENV", s"MESOS_JAVA_NATIVE_LIBRARY /usr/local/lib/libmesos-$mesosVersion.so"),
    Cmd("ENV", s"MESOS_LOG_DIR /var/log/mesos")
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
  // java image based on ubuntu trusty rather than debian jessie (to use mesosphere distros)
  // build it like this:
  // ```
  // docker build -t="dockerfile/ubuntu" github.com/dockerfile/ubuntu
  // git clone https://github.com/dockerfile/java.git
  // cd java
  // cd openjdk-7-jdk
  // docker build -t="dockerfile/java:openjdk-7-jdk" .
  // ```
  val baseImage    = getString("docker.baseImage", "dockerfile/java:openjdk-7-jdk")

  val commands     = Try { asCmdSeq(cfg.getConfigList("docker.commands").asScala.toSeq) }.getOrElse( defaultCommands )
  val volumes      = Try { cfg.getStringList("docker.volumes").asScala.toSeq }.getOrElse( defaultVolumes )
  val registry     = Some(getString("docker.registry", "andypetrella"))
  val ports        = Try { cfg.getIntList("docker.ports").asScala.toSeq.map { e => e.intValue() } }.getOrElse( Seq(9000, 9443) )
}