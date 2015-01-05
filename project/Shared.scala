import sbt._
import Keys._

import Dependencies._

object Shared {
  lazy val sparkVersion = SettingKey[String]("x-spark-version")

  lazy val hadoopVersion = SettingKey[String]("x-hadoop-version")

  lazy val sharedSettings:Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.10.4",
    sparkVersion  := defaultSparkVersion,
    hadoopVersion := defaultHadoopVersion
  )

  lazy val sparkSettings:Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (sparkVersion, hadoopVersion) { (sv, hv) =>
      val libs = Seq(
        guava,
        sparkRepl(sv),
        sparkSQL(sv),
        hadoopClient(hv)
      )
      libs
    }
  )
}