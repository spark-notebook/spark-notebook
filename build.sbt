import Dependencies._
import Shared._
import sbtbuildinfo.Plugin._

organization := MainProperties.organization

name := MainProperties.name

scalaVersion := defaultScalaVersion

val SparkNotebookSimpleVersion = "0.7.0-SNAPSHOT"

version in ThisBuild <<= (scalaVersion, sparkVersion, hadoopVersion, withHive, withParquet) { (sc, sv, hv, h, p) =>
  s"$SparkNotebookSimpleVersion-scala-$sc-spark-$sv-hadoop-$hv" + (if (h) "-with-hive" else "") + (if (p) "-with-parquet" else "")
}

updateOptions := updateOptions.value.withCachedResolution(true)

maintainer := DockerProperties.maintainer //Docker

enablePlugins(UniversalPlugin)

enablePlugins(DockerPlugin)

enablePlugins(GitVersioning)

enablePlugins(GitBranchPrompt)

import uk.gov.hmrc.gitstamp.GitStampPlugin._

net.virtualvoid.sbt.graph.Plugin.graphSettings

import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._

import com.typesafe.sbt.packager.docker._

dockerBaseImage := DockerProperties.baseImage

dockerCommands ++= DockerProperties.commands

dockerExposedVolumes ++= DockerProperties.volumes

dockerExposedPorts ++= DockerProperties.ports

dockerRepository := DockerProperties.registry //Docker

packageName in Docker := "spark-notebook"

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

parallelExecution in Test in ThisBuild := false

// these java options are for the forked test JVMs
javaOptions in ThisBuild ++= Seq("-Xmx512M", "-XX:MaxPermSize=128M")


/*
  adding nightly build resolver like
  https://repository.apache.org/content/repositories/orgapachespark-1153
  this props will only need the number 1153 at the end
*/
val searchSparkResolver = Option(sys.props.getOrElse("spark.resolver.search", "false")).get.toBoolean
val sparkResolver = Option(sys.props.getOrElse("spark.resolver.id", null))

resolvers in ThisBuild ++= Seq(
  Resolver.mavenLocal,
  Resolver.typesafeRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.typesafeIvyRepo("releases"),
  Resolver.typesafeIvyRepo("snapshots"),
  "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos",
  // docker
  "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
) ++ ((sparkResolver, searchSparkResolver, sparkVersion.value) match {
  case (Some(x), _, sv) => Seq(
                    ("spark " + x) at ("https://repository.apache.org/content/repositories/orgapachespark-"+x)
                  )
  case (None, true, sv)  =>
    println(s"""|
    |**************************************************************
    | SEARCHING for Spark Nightly repo for version ~ $sv
    |**************************************************************
    |""".stripMargin)
    import scala.io.Source.fromURL
    val mainPage = "https://repository.apache.org/content/repositories/"
    val repos = fromURL(mainPage).mkString
    val c = repos.split("\n").toList.filter(!_.contains("<link")).mkString
    val r = scala.xml.parsing.XhtmlParser(scala.io.Source.fromString(c))
    val as = r \\ "a"
    val sparks:List[Option[String]] = as.filter(_.toString.contains("orgapachespark")).map(_.attribute("href").map(_.head.text)).toList
    val sparkRepos = sparks.collect { case Some(l) =>
      val id = l.reverse.tail.takeWhile(_.isDigit).reverse.toInt
      val u = l + "org/apache/spark/spark-core_2.10/"
      val repo = fromURL(u).mkString
      val c = repo.split("\n").toList.filter(!_.contains("<link")).mkString
      val r = scala.xml.parsing.XhtmlParser(scala.io.Source.fromString(c))
      val v = (r \\ "table" \\ "a").map(_.text).filter(_ != "Parent Directory").head.replace("/", "")
      (l, id, v)
    }
    .groupBy(_._3).mapValues(_.sortBy(_._2).last._1)
    .map{ case (v, url) => (v == sv, v, url) }
    println("======================================================================== ")
    println("Found these repos")
    println(sparkRepos.map{case (c, v, url) => s"${if(c) "[x]" else "[ ]" } $v: $url"}.mkString("\n"))

    sparkRepos.filter(_._1).map{ case (_, v, url) => ("spark " + v) at url }

  case (None, false, _) => Nil
})

EclipseKeys.skipParents in ThisBuild := false

compileOrder := CompileOrder.Mixed

publishMavenStyle := false

javacOptions ++= Seq("-Xlint:deprecation", "-g")

scalacOptions ++= Seq("-deprecation", "-feature")

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

//scriptClasspath := Seq("*")

scriptClasspath in batScriptReplacements := Seq("*")

batScriptExtraDefines += {
  "set \"APP_CLASSPATH=%CLASSPATH_OVERRIDES%;%YARN_CONF_DIR%;%HADOOP_CONF_DIR%;%EXTRA_CLASSPATH%;%APP_CLASSPATH%\""
}

val ClasspathPattern = "declare -r app_classpath=\"(.*)\"\n".r

bashScriptDefines := bashScriptDefines.value.map {
  case ClasspathPattern(classpath) =>
    s"""declare -r app_classpath="$${CLASSPATH_OVERRIDES}:$${YARN_CONF_DIR}:$${HADOOP_CONF_DIR}:$${EXTRA_CLASSPATH}:${classpath}"\n"""
  case entry => entry
}

dependencyOverrides += log4j

dependencyOverrides += guava

enablePlugins(DebianPlugin)

sharedSettings

libraryDependencies ++= playDeps

libraryDependencies ++= List(
  akka,
  akkaRemote,
  akkaSlf4j,
  cache,
  commonsIO,
  // ↓ to fix java.lang.IllegalStateException: impossible to get artifacts when data has
  //   not been loaded. IvyNode = org.apache.commons#commons-exec;1.1
  //   encountered when using hadoop "2.0.0-cdh4.2.0"
  commonsExec,
  commonsCodec,
  //scala stuffs
  "org.scala-lang" % "scala-library" % defaultScalaVersion,
  "org.scala-lang" % "scala-reflect" % defaultScalaVersion,
  "org.scala-lang" % "scala-compiler" % defaultScalaVersion
)

//for aether and compensating for 2.11 modularization
libraryDependencies <++= scalaBinaryVersion {
  case "2.10" => Nil
  case "2.11" => List(ningAsyncHttpClient,
    "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  )
}

lazy val sparkNotebook = project.in(file(".")).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
  .aggregate(tachyon, subprocess, observable, common, spark, kernel)
  .dependsOn(tachyon, subprocess, observable, common, spark, kernel)
  .settings(sharedSettings: _*)
  .settings(
    bashScriptExtraDefines <+= (version, scalaBinaryVersion, scalaVersion, sparkVersion, hadoopVersion, withHive, withParquet) map { (v, sbv, sv, pv, hv, wh, wp) =>
      """export ADD_JARS="${ADD_JARS},${lib_dir}/$(ls ${lib_dir} | grep common.common | head)""""
    },
    mappings in Universal ++= directory("notebooks"),
    mappings in Docker ++= directory("notebooks")
  )
  .settings(includeFilter in(Assets, LessKeys.less) := "*.less")
  .settings(unmanagedSourceDirectories in Compile <<= (scalaSource in Compile)(Seq(_))) //avoid app-2.10 and co to be created
  .settings(initialCommands += ConsoleHelpers.cleanAllOutputs)
  .settings(
    git.useGitDescribe := true,
    git.baseVersion := SparkNotebookSimpleVersion
  )
  .settings(
    gitStampSettings: _*
  )

lazy val subprocess = project.in(file("modules/subprocess"))
  .settings(libraryDependencies ++= playDeps)
  .settings(
    libraryDependencies ++= {
      Seq(
        akka,
        akkaRemote,
        akkaSlf4j,
        commonsIO,
        commonsExec,
        log4j
      )
    }
  )
  .settings(sharedSettings: _*)
  .settings(sparkSettings: _*)


lazy val observable = Project(id = "observable", base = file("modules/observable"))
  .dependsOn(subprocess)
  .settings(
    libraryDependencies ++= Seq(
      akkaRemote,
      akkaSlf4j,
      slf4jLog4j,
      rxScala
    )
  )
  .settings(sharedSettings: _*)

lazy val common = Project(id = "common", base = file("modules/common"))
  .dependsOn(observable)
  .settings(
    libraryDependencies ++= Seq(
      akka,
      log4j,
      scalaZ
    ),
    libraryDependencies ++= depsToDownloadDeps(scalaBinaryVersion.value, sbtVersion.value),
    // plotting functionality
    libraryDependencies ++= Seq(
      bokeh
    ), // ++ customJacksonScala
    unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value),
    unmanagedSourceDirectories in Compile +=
      (sourceDirectory in Compile).value / ((sparkVersion.value.takeWhile(_ != '-').split("\\.").toList match {
        case "1"::x::_ if x.toInt < 3 => "pre-df"
        case x                        => "post-df"
      }))
  )
  .settings(
    wispSettings
  )
  .settings(
    gisSettings
  )
  .settings(sharedSettings: _*)
  .settings(sparkSettings: _*)
  .settings(buildInfoSettings: _*)
  .settings(
    sourceGenerators in Compile <+= buildInfo,
    buildInfoKeys :=  Seq[BuildInfoKey](
                        "sparkNotebookVersion" → SparkNotebookSimpleVersion,
                        scalaVersion,
                        sparkVersion,
                        hadoopVersion,
                        withHive,
                        withParquet,
                        jets3tVersion,
                        jlineDef,
                        sbtVersion,
                        git.formattedShaVersion,
                        BuildInfoKey.action("buildTime") {
                          val formatter = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                          formatter.format(new java.util.Date(System.currentTimeMillis))
                        }
                      ),
    buildInfoPackage := "notebook"
  )

lazy val spark = Project(id = "spark", base = file("modules/spark"))
  .dependsOn(common, subprocess, observable)
  .settings(
    libraryDependencies ++= Seq(
      akkaRemote,
      akkaSlf4j,
      slf4jLog4j,
      commonsIO
    ),
    libraryDependencies ++= Seq(
      jlineDef.value._1 % "jline" % jlineDef.value._2,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    ),
    unmanagedSourceDirectories in Compile += {
      implicit val versionCompare = Ordering.apply[(Int, Int, Int)]
      def folder(v:String, sv:String) = {
          val tsv = sv match { case extractVs(v,m,p) => (v.toInt,m.toInt,p.toInt) }

          val scalaVerDir = (sourceDirectory in Compile).value / ("scala_" + v)

          tsv match {
            // if spark.version < 1.5.0 use spark-pre1.5
            case _ if versionCompare.lt(tsv, (1, 5, 0)) => scalaVerDir / "spark-pre1.5"
            case _ => scalaVerDir / "spark-last"
          }
      }
      (scalaBinaryVersion.value, sparkVersion.value.takeWhile(_ != '-')) match {
        case (v, sv) if v startsWith "2.10" => folder("2.10", sv)
        case (v, sv) if v startsWith "2.11" => folder("2.11", sv)
        case (v, sv) => throw new IllegalArgumentException("Bad scala version: " + v)
      }
    }
  )
  .settings(sharedSettings: _*)
  .settings(sparkSettings: _*)

lazy val tachyon = Project(id = "tachyon", base = file("modules/tachyon"))
  .settings(sharedSettings: _*)
  .settings(tachyonSettings: _*)

lazy val kernel = Project(id = "kernel", base = file("modules/kernel"))
  .dependsOn(common, subprocess, observable, spark)
  .settings(
    libraryDependencies ++= Seq(
      akkaRemote,
      akkaSlf4j,
      slf4jLog4j,
      commonsIO
    ),
    unmanagedSourceDirectories in Compile += (sourceDirectory in Compile).value / ("scala-" + scalaBinaryVersion.value)
  )
  .settings(sharedSettings: _*)
