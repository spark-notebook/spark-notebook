import java.util.Properties

import Dependencies._
import com.typesafe.config.{Config, ConfigFactory}
import sbt._

import scala.util.Try

trait BuildConf {
  def fileName:String

  import scala.collection.JavaConverters._

  lazy val buildConfig = file(fileName)
  lazy val cfg = Try { ConfigFactory.parseFile(buildConfig) }.getOrElse(ConfigFactory.parseProperties(new Properties()))

  def getString(k:String, d:String):String = Try { cfg.getString(k) }.getOrElse(d)
}