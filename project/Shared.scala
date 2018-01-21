import Dependencies._
import sbt.Keys._
import sbt._

object Shared {
  lazy val sparkVersion = SettingKey[String]("x-spark-version")

  lazy val hadoopVersion = SettingKey[String]("x-hadoop-version")

  lazy val jets3tVersion = SettingKey[String]("x-jets3t-version")

  lazy val jlineDef = SettingKey[(String, String)]("x-jline-def")

  lazy val withHive = SettingKey[Boolean]("x-with-hive")

  lazy val sharedSettings: Seq[Def.Setting[_]] = Seq(
    publishArtifact in Test := false,
    publishMavenStyle := true,

    organization := MainProperties.organization,
    scalaVersion := defaultScalaVersion,
    sparkVersion := defaultSparkVersion,
    hadoopVersion := defaultHadoopVersion,
    jets3tVersion := defaultJets3tVersion,
    jlineDef := (if (defaultScalaVersion.startsWith("2.10")) {
      ("org.scala-lang", defaultScalaVersion)
    } else {
      ("jline", "2.12")
    }),
    withHive := defaultWithHive,
    libraryDependencies += guava,
    libraryDependencies ++= jodaTime,
    libraryDependencies ++= netty,
    excludeDependencies ++= nettyExclusions,
    libraryDependencies += jerseyClient
  )

  val gisSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= geometryDeps
  )

  val repl: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += sparkRepl(sparkVersion.value)
  )

  val hive: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= {
      if(withHive.value) Seq(sparkHive(sparkVersion.value)) else Seq.empty
    }
  )

  val yarnWebProxy: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= {
      val hv = hadoopVersion.value
      if (!hv.startsWith("1")) Seq(yarnProxy(hv)) else Seq.empty
    }
  )

  lazy val sparkSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= {
      val jets3tVersion = sys.props.get("jets3t.version") match {
        case Some(jv) => jets3t(Some(jv), None)
        case None => jets3t(None, Some(hadoopVersion.value))
      }

      val jettyVersion = "8.1.14.v20131031"

      val libs = Seq(
        sparkCore(sparkVersion.value),
        sparkYarn(sparkVersion.value),
        sparkSQL(sparkVersion.value),
        hadoopClient(hadoopVersion.value),
        jets3tVersion,
        commonsCodec
      ) ++ (
            if (!scalaVersion.value.startsWith("2.10")) {
              // in 2.11
              //Boot.scala → HttpServer → eclipse
              // eclipse → provided boohooo :'-(
              Seq(
                "org.eclipse.jetty" % "jetty-http"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
                "org.eclipse.jetty" % "jetty-servlet"      % jettyVersion,
                "org.eclipse.jetty" % "jetty-util"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-security"     % jettyVersion,
                "org.eclipse.jetty" % "jetty-plus"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-server"       % jettyVersion
              )
            } else Nil
          ) ++ sparkMesos(sparkVersion.value)
      libs
    }
  ) ++ repl ++ hive ++ yarnWebProxy
}
