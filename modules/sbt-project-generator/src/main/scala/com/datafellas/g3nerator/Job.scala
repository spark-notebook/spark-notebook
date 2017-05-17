package com.datafellas.g3nerator

import java.io.File

import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.datafellas.g3nerator.model._
import com.datafellas.g3nerator.model.Project
import com.datafellas.g3nerator.FileUtils._
import com.datafellas.utils._
import notebook.Notebook
import com.datafellas.g3nerator.model.Artifact.State._
import notebook.NBSerializer.CodeCell
import notebook.util.ZipArchiveUtil

import scala.util.{Success, Try}

class Job( val project: Project,
           val repo: File,
           val root: File ) {

  import Job._
  //[gm!] for this to work, notebook metadata must be mandatory
  private[this] val snb: Notebook = project.snb
  val metadata = snb.metadata.get

  private[this] val LOG = org.slf4j.LoggerFactory.getLogger(getClass.getSimpleName)

  val materializer = new FileSystemMaterializer

  val snConfig = metadata.sparkNotebook.getOrElse(Map.empty)
  // Always use version with which SN is built for now
  // def pickVersion(key: String, snVersion: String) = snConfig.getOrElse(key, snVersion)
  def pickVersion(key: String, snBuildInfoVersion: String) = snBuildInfoVersion

  val scalaVersion = pickVersion("scalaVersion", notebook.BuildInfo.scalaVersion)
  val majorScalaVersion = scalaVersion.split("\\.").take(2).mkString(".")
  val sparkNotebookVersion = pickVersion("sparkNotebookVersion", notebook.BuildInfo.sparkNotebookVersion)
  val sparkVersion = pickVersion("xSparkVersion", notebook.BuildInfo.xSparkVersion)
  val hadoopVersion = pickVersion("xHadoopVersion", notebook.BuildInfo.xHadoopVersion)

  val prodSparkVersion = project.productionConfig.map(_.sparkVersion).getOrElse(sparkVersion)
  val prodHadoopVersion = project.productionConfig.map(_.hadoopVersion).getOrElse(hadoopVersion)
  val customDeps = metadata.customDeps.getOrElse(Nil) ::: List(
    """- com.typesafe.akka % _ % _""",
    """- com.google.guava % _ % _"""
  )
  val uuid = snb.metadata.map(_.id).getOrElse("id-not-found")
  val notebookName = snb.normalizedName

  val library = Library(notebookName, project.config.pkg, project.config.version, majorScalaVersion)

  lazy val nbDepencyResolver = new SbtDependencyResolver(snb, sparkVersion, project.dependencyConfig)

  //[gm!] looks like notebook raw content must be mandatory
  lazy val longLived = snb.rawContent.exists(_.contains("StreamingContext"))

  // build artifacts
  lazy val `root/out` = root("out")
  lazy val `root/compile.statuscode` = root("compile.statuscode")
  lazy val `root/gen.sh` = root("gen.sh")
  lazy val `root/build.sbt` = root("build.sbt")
  lazy val `root/project`= root / "project"
  lazy val `root/project/build.properties` = `root/project`("build.properties")
  lazy val `root/project/plugins.sbt` = `root/project`("plugins.sbt")
  // code/config artifacts
  lazy val `root/src` = root / "src"
  lazy val `root/src/main/scala` = root / "src/main/scala"
  lazy val `root/src/main/scala/App.scala` = `root/src/main/scala`("App.scala")
  lazy val `root/src/main/scala/Classes.scala` = `root/src/main/scala`("Classes.scala")
  lazy val `root/src/main/resources` = root/ "src/main/resources/"
  lazy val `root/src/main/resources/notebook.snb` = `root/src/main/resources`("notebook.snb")
  lazy val `root/src/main/resources/application.conf` = `root/src/main/resources`("application.conf")
  lazy val `root/target` = root / "target"
  lazy val `root/target/debian_pkg.deb` = `root/target`(library.debianPackage)
  // mocked widgets / charts
  lazy val `root/src/main/scala/MockedCharts.scala` =  `root/src/main/scala`("MockedCharts.scala")

  // spark artifacts
  lazy val `root/spark-lib` = root / "spark-lib"
  lazy val `root/sources.zip` = root("sources.zip")

  // job content
  lazy val build = buildSBT()
  lazy val createSparkContextCode = buildCreateSparkContextCode()
  lazy val mainCode = buildMainCode()
  lazy val classCode = buildClasses()

  lazy val sourceArchiveFiles = Seq(
    `root/build.sbt`,
    `root/project/build.properties`,
    `root/project/plugins.sbt`,
    `root/spark-lib`,
    //  `root/target`(library.jarTargetPath),
    `root/src`
  )
  lazy val artifactsToClean = sourceArchiveFiles ++ Seq(
    `root/compile.statuscode`,
    `root/sources.zip`,
    `root/out` // clean old logs
  )


  private var generated = false
  def isGenerated = generated

  def artifacts(local:Boolean): List[Artifact[Unmaterialized]] = {
    LOG.info("Generating job artifacts")
    val jobArtifacts = List(Artifact(`root/project/plugins.sbt`, Job.Build.Sbt.plugins),
      Artifact(`root/project/build.properties`, Job.Build.properties),
      Artifact(`root/build.sbt`, build),
      Artifact(`root/src/main/scala/App.scala`, app()),
      Artifact(`root/src/main/scala/Classes.scala`, classes()),
      Artifact(`root/src/main/scala/MockedCharts.scala`, mockedChartsCode),
      Artifact(`root/src/main/resources/application.conf`, appConfig()),
      Artifact(`root/gen.sh`, genScript(local))
    )
    snb.rawContent.foldLeft(jobArtifacts)((artifacts, raw) =>
      Artifact(`root/src/main/resources/notebook.snb`, raw) :: artifacts)
  }

  def materialize(artifacts: List[Artifact[Unmaterialized]]): List[Try[Artifact[Materialized]]] = {
    artifacts.map(artifact => artifact.materialize(materializer))
  }

  def cleanOldArtifacts(): Unit = {
    import org.apache.commons.io.FileUtils
    artifactsToClean.foreach(FileUtils.deleteQuietly)
  }

  def writeSourcesZip(): Unit = {
    println(s"zip file: ${`root/sources.zip`}")
    println(s"zip contents: ${sourceArchiveFiles}")

    val tmpFileName = "sources.zip.tmp"
    val tmpFile = root(tmpFileName)
    tmpFile.exists() && tmpFile.delete()

    ZipArchiveUtil.createArchiveRecursively(
      rootPath = root.getAbsolutePath,
      inputFiles = sourceArchiveFiles.filter(_.exists).map(_.getAbsolutePath),
      outputFilename = tmpFileName
    )
    tmpFile.renameTo(`root/sources.zip`)
  }

  def generateOnly(local: Boolean): Try[Unit] = {
    val artfs = artifacts(local)
    val materializedArtifacts = materialize(artfs)
    materializedArtifacts.foldLeft(Success(()):Try[Unit]){(res, artf)=> res.flatMap(e=> artf.map(_=>()))}
  }

  def run(genScript: File, output: File): Future[Unit] = {
    genScript.setExecutable(true)
    import sys.process._
    import ExecutionContext.Implicits.global
    Future {
      s"bash ${genScript.getAbsolutePath}" #>> output lines_! ProcessLogger(out => LOG.info(notebookName + "::" + out), err => LOG.error(err))
      ()
    }
  }

  // This needs another refactoring round for error handling
  def publish(local: Boolean): Future[Unit] =  {
    cleanOldArtifacts()
    val artfs = artifacts(local)
    val materializedArtifacts = materialize(artfs)
    val maybeBuildScript = materializedArtifacts.collect{
      case Success(artf) if artf.fd == `root/gen.sh` => artf.fd
    }.headOption
    val buildResult = maybeBuildScript.map {script => run(script, `root/out`)}
      .getOrElse(Future.failed(new RuntimeException("Could not generate gen script")))

    import ExecutionContext.Implicits.global
    buildResult.andThen { case _ => writeSourcesZip() }
  }


  def indentLines(str: String, nChars: Int = 2) = {
    val indent = " " * nChars
    str
      .split("\n")
      .map(indent + _)
      .mkString("\n")
      .trim
  }

  def app() = {
    s"""|
    |package ${library.classPkg}
    |
    |object Main {
    |
    |  def main(args:Array[String]):Unit = {
    |    // spark context
    |    ${indentLines(createSparkContextCode, nChars = 4)}
    |
    |    // main code
    |    ${indentLines(mainCode, nChars = 4)}
    |  }
    |}
    |""".stripMargin
  }

  def classes() = {
    s"""|
    |package ${library.classPkg}
    |
    |$classCode
    |
    |""".stripMargin
  }

  def genScript(local: Boolean) = {
    val rootPath = root.getAbsolutePath

    //    val publishDocker = if (Option(project.config.dockerRepo).getOrElse("").trim.nonEmpty) {
    //      s"""
    //         |
    //         |echo "publishing docker"
    //         |docker push ${project.config.dockerRepo}/${DebianName(notebookName).name}:${project.config.version}
    //         |
    //         |""".stripMargin
    //    }
    //    else ""

    val sbt = s"${project.dependencyConfig.sbtHome}/bin/sbt"

    // val publishCmd = s"publish${project.dependencyConfig.artifactory.map(_ => "").getOrElse("Local")}"
    val publishCmd = project.dependencyConfig.artifactory match {
      case Some(_) => "publish"
      case None => "package"
    }

    val publishJarCmd =
      s"""
         |echo "compiling/publishing a jar"
         |$sbt -Dspark.version=${prodSparkVersion} -Dhadoop.version=${prodHadoopVersion} clean $publishCmd
         |echo "$$?" > compile.statuscode
       """.stripMargin

    val pkgPath = `root/target/debian_pkg.deb`.getAbsolutePath
    val packageDebCmd = ""
    //      s"""
    //         |echo "building debian"
    //         |$sbt -Dspark.version=${prodSparkVersion} -Dhadoop.version=${prodHadoopVersion} debian:packageBin
    //         |echo "package: $pkgPath "
    //       """.stripMargin

    s"""|
        |#!/bin/bash
        |
        |echo "Start gen.sh"
        |echo $$(date)
        |echo "At directory `pwd`"
        |
        |export JAVA_HOME=${project.dependencyConfig.jdkHome}
        |export JDK_HOME=${project.dependencyConfig.jdkHome}
        |export PATH=${project.dependencyConfig.sbtHome}/bin:${project.dependencyConfig.jdkHome}/bin:$$PATH
        |
        |echo "cd ${root.getAbsolutePath}"
        |
        |cd ${root.getAbsolutePath}
        |
        |${publishJarCmd}
        |
        |${packageDebCmd}
        ||
        |echo "End gen.sh"
        |echo $$(date)
        |
      |""".stripMargin.trim
  }

  def buildSBT() = {
    val deps = nbDepencyResolver.libraryDependenciesCode(customDeps)
    val customResolversCode = nbDepencyResolver.resolversCode

    val addJvmArgs = snb.metadata.get.customArgs.getOrElse(Nil).map { a =>
      s"""bashScriptExtraDefines += \"\"\"addJava "$a"\"\"\""""
    }.mkString("\n\n")

    val artifactoryPublish:String = project.dependencyConfig.artifactory.map { case ((pull, push), credOpt) =>
      val cred = credOpt.map { case (user, password) =>
        val host = new java.net.URI(push).getHost
        s"""credentials += Credentials("Artifactory Realm", "$host", "$user", "$password")"""
      }.getOrElse("")

      val addAssembly = project.productionConfig.map { _ =>
        "addArtifact(artifact in (Compile, assembly), assembly).settings"
      }.getOrElse("")

      val publish = s"""|
      |$addAssembly
      |
      |publishTo := Some("Artifactory PUSH Realm" at "$push")
      |
      |publishMavenStyle := true
      |
      |$cred
      |
      |""".stripMargin.trim

      publish
    }.getOrElse("")

    val artifactoryResolver:String = project.dependencyConfig.artifactory.map { case ((pull, push), _) =>
      s"""resolvers += "Adastyx PULL Artifactory" at "${pull}""""
    }.getOrElse("")

    val providedArtifact = "% \"provided\""

    val sparkProvided = ""
    val hadoopProvided = ""

    s"""|
    |organization := "${library.group}"
    |
    |name := "${library.name}"
    |
    |version := "${library.version}"
    |// append scala version to artifact name(s)
    |crossPaths := true
    |
    |scalaVersion := "${scalaVersion}"
    |
    |maintainer := "${project.config.maintainer}" //Docker
    |
    |resolvers ++= ${customResolversCode}
    |
    |net.virtualvoid.sbt.graph.Plugin.graphSettings
    |
    |enablePlugins(UniversalPlugin)
    |
    |enablePlugins(DockerPlugin)
    |
    |enablePlugins(JavaAppPackaging)
    |
    |import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
    |
    |import com.typesafe.sbt.packager.docker._
    |
    |dockerBaseImage := "${project.dependencyConfig.dockerBaseImage}"
    |
    |dockerExposedPorts := Seq(9000, 9443)
    |
    |daemonUser in Docker := "root"
    |
    |packageName in Docker := "${library.classPkg}"
    |
    |mappings in Docker ++= directory("spark-lib")
    |
    |mappings in Universal ++= directory("spark-lib")
    |
    |resolvers += Resolver.mavenLocal
    |
    |resolvers += Resolver.typesafeRepo("releases")
    |
    |resolvers += "cloudera" at "${project.dependencyConfig.resolverCloudera}"
    |
    |$artifactoryResolver
    |
    |credentials += Credentials(Path.userHome / ".bintray" / ".credentials")
    |
    |resolvers += Resolver.url("bintray-data-fellas-maven", url("${project.dependencyConfig.resolverBintrayDataFellasMvn}"))(Resolver.ivyStylePatterns)
    |
    |dockerCommands ++= Seq(Cmd("ENV", "SPARK_HOME \\"\\""))
    |
    |dockerRepository := Some("${project.config.dockerRepo}") //Docker
    |
    |enablePlugins(DebianPlugin)
    |
    |name in Debian := "${DebianName(notebookName).name}"
    |
    |maintainer in Debian := "Data Fellas"
    |
    |packageSummary in Debian := "Data Fellas Generated Job"
    |
    |packageDescription := "Generated Job by Spark-notebook"
    |
    |debianPackageDependencies in Debian += "java8-runtime-headless"
    |
    |serverLoading in Debian := com.typesafe.sbt.packager.archetypes.ServerLoader.Upstart
    |
    |daemonUser in Linux := "root"
    |
    |daemonGroup in Linux := "root"
    |
    |bashScriptExtraDefines += "export SPARK_HOME=\\"\\""
    |
    |
    |$addJvmArgs
    |
    |val sparkVersion  = sys.env.get("SPARK_VERSION") .orElse(sys.props.get("spark.version")) .getOrElse("$sparkVersion")
    |
    |val hadoopVersion = sys.env.get("HADOOP_VERSION").orElse(sys.props.get("hadoop.version")).getOrElse("$hadoopVersion")
    |
    |// TODO: needed only if you use some of spark-notebook code
    |// (most likely you don't want to use this, otherwise  you'd need to publishLocal the SN libs)
    |// libraryDependencies += "io.kensu" %% "common" % (sparkVersion + "_${sparkNotebookVersion}") excludeAll(
    |//    ExclusionRule("org.apache.hadoop"),
    |//    ExclusionRule("org.apache.spark")
    |// )
    |
    |libraryDependencies += "com.typesafe" % "config" % "1.3.1"
    |
    |// you might not need all of the Spark jars below
    |libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion $sparkProvided excludeAll(
    |    ExclusionRule("org.apache.hadoop"),
    |    ExclusionRule("org.apache.ivy", "ivy")
    |  )
    |
    |libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion $sparkProvided excludeAll(
    |    ExclusionRule("org.apache.hadoop"),
    |    ExclusionRule("org.apache.ivy", "ivy")
    |  )
    |
    |libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion $sparkProvided excludeAll(
    |  ExclusionRule("org.apache.hadoop")
    |)
    |
    |libraryDependencies += "org.apache.spark" %% "spark-yarn" % sparkVersion $sparkProvided excludeAll(
    |  ExclusionRule("org.apache.hadoop"),
    |  ExclusionRule("org.apache.ivy", "ivy")
    |  )
    |
    |libraryDependencies += "org.apache.spark" %% "spark-hive" % sparkVersion  $sparkProvided excludeAll(
    |    ExclusionRule("org.apache.hadoop"),
    |    ExclusionRule("org.apache.ivy", "ivy"),
    |    ExclusionRule("javax.servlet", "servlet-api"),
    |    ExclusionRule("org.mortbay.jetty", "servlet-api")
    |  )
    |
    |libraryDependencies += "org.apache.hadoop" % "hadoop-client" % hadoopVersion  $hadoopProvided excludeAll(
    |    ExclusionRule("org.apache.commons", "commons-exec"),
    |    ExclusionRule("commons-codec", "commons-codec"),
    |    ExclusionRule("com.google.guava", "guava"),
    |    ExclusionRule("javax.servlet")
    |  )
    |
    |libraryDependencies += "org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % hadoopVersion  $hadoopProvided excludeAll(
    |      ExclusionRule("org.apache.commons", "commons-exec"),
    |      ExclusionRule("commons-codec", "commons-codec"),
    |      ExclusionRule("com.google.guava", "guava"),
    |      ExclusionRule("javax.servlet")
    |  )
    |
    |libraryDependencies += "net.java.dev.jets3t" % "jets3t" % "0.9.0" force()
    |
    |libraryDependencies += "com.google.guava" % "guava" % "16.0.1" force()
    |
    |$deps
    |
    |//asssembly
    |// skip test during assembly
    |test in assembly := {}
    |
    |//main class
    |mainClass in assembly := Some("${project.config.pkg}.Main")
    |
    |artifact in (Compile, assembly) ~= { art =>
    |  art.copy(`classifier` = Some("assembly"))
    |}
    |
    |$artifactoryPublish
    |
    |// merging files... specially application.conf!
    |assemblyMergeStrategy in assembly := {
    |  case PathList("javax", "servlet",          xs @ _*) => MergeStrategy.first
    |  case PathList("org",   "apache",           xs @ _*) => MergeStrategy.first
    |  case PathList("org",   "fusesource",       xs @ _*) => MergeStrategy.first
    |  case PathList("org",   "slf4j",            xs @ _*) => MergeStrategy.first
    |  case PathList("com",   "google",           xs @ _*) => MergeStrategy.first
    |  case PathList("play",  "core",             xs @ _*) => MergeStrategy.first
    |  case PathList("javax", "xml",              xs @ _*) => MergeStrategy.first
    |  case PathList("com",   "esotericsoftware", xs @ _*) => MergeStrategy.first
    |  case PathList("xsbt",                      xs @ _*) => MergeStrategy.first
    |  case PathList("META-INF", "MANIFEST.MF"           ) => MergeStrategy.discard
    |  case PathList("META-INF",                  xs @ _*) => MergeStrategy.first
    |  case "application.conf"                             => MergeStrategy.concat
    |  case "module.properties"                             => MergeStrategy.first
    |  case PathList(ps @ _*) if ps.last endsWith ".html"  => MergeStrategy.discard
    |  case PathList(ps @ _*) if ps.last endsWith ".thrift"  =>  MergeStrategy.first
    |  case PathList(ps @ _*) if ps.last endsWith ".xml"  =>  MergeStrategy.first
    |  case x =>
    |    val oldStrategy = (assemblyMergeStrategy in assembly).value
    |    oldStrategy(x)
    |}
    |
    |aggregate in update := false
    |
    |updateOptions := updateOptions.value.withCachedResolution(true)
    |
    |""".stripMargin
  }

  def appConfig(): String = {
    val fromMD = snb.metadata.flatMap { meta =>
      val customSparkConf = meta.customSparkConf.flatMap { customConf =>
        Reads.map[String].reads(customConf).asOpt
      }.map { kvMap =>
        kvMap.map { case (k, v) =>
          val value = v.toString.replaceAll("\"", "\\\\\"")
          s""" $k : "$value"  """
        }.mkString("\n")
      }

      val customVars = meta.customVars.map { customVars =>
        customVars.map { case (k, v) => s""" $NotebookVarsPrefix.$k : "$v" """ }.mkString("\n")
      }

      val config = List(customSparkConf, customVars).flatten.mkString("\n")
      if (config.isEmpty) None else Some(config)
    }

    List(fromMD, Some("#-- End Config --")).flatten.mkString("\n")
  }

  def downloadCustomDependencies(): List[String] = {
    // copy all customDeps to the spark-lib directory
    // FIXME: makes sense for locally installed dependencies, but it would be better to leave the regular custom dependencies for SBT
    LOG.info("Custom deps to resolve and download:" + snb.metadata.get.customDeps.getOrElse(Nil))
    val jars: Try[List[String]] = nbDepencyResolver.resolveJars(snb.metadata.get.customDeps.getOrElse(Nil), repo)
    LOG.info("Downloaded deps for job:\n" + jars)
    val libJars = jars.get.map { jar =>
      import scalax.io._
      import Resource._
      val name = new File(jar).getName
      fromFile(jar) copyDataTo fromFile(`root/spark-lib`(name))
      name
    }
    libJars
  }

  def executorJarsCode(downloadedJars: Seq[String]) = {
    val downloadedJarsArray = downloadedJars.map { j => s""" "$j" """ }.mkString("Array[String](", ",", ")")

    // FIXME: this is valid only for 'debian' & 'dist/stage/package' builds only (!)
    val currentProjectJarsArray = s"""Array("lib/${library.jarInDeb}", "${library.jarInTargetPath}")"""

    s"""|
        |def setExecutorJars() = {
        |  val currentProjectJars = $currentProjectJarsArray.map{j => new java.io.File(j)}.filter(_.exists()).map(_.getAbsolutePath)
        |  val sparkLibDir = new java.io.File("spark-lib")
        |  val fromProjectJars = $downloadedJarsArray.map{j => new java.io.File(sparkLibDir, j).getAbsolutePath}
        |  val jarsArray = (sparkConf.get("spark.jars", "").split(",").toArray ++ currentProjectJars ++ fromProjectJars).distinct.filter(!_.isEmpty)
        |  println("Add Jars: \\n" + jarsArray.mkString("\\n"))
        |  sparkConf.setJars(jarsArray)
        |}
        |""".stripMargin
  }

  def buildCreateSparkContextCode() = {
    val downloadedJars = downloadCustomDependencies()

    s"""|
    |import org.apache.spark.{SparkContext, SparkConf}
    |import org.apache.spark.SparkContext._
    |import org.apache.spark.rdd._
    |import org.apache.spark.sql._
    |import org.apache.spark.sql.functions._
    |import com.typesafe.config._
    |import scala.collection.JavaConverters._
    |import scala.util.Try
    |
    |
    |// Spark notebook widgets (can be removed if you do not use them)
    |// Dummy implementation of the most common ones (to avoid shipping 80+ MB of spark-notebook jars)
    |import notebook.front.widgets.charts._
    |import notebook.front.widgets.charts.MockedCharts._
    |
    |// Create spark configuration holder
    |val sparkConf = new SparkConf()
    |
    |// Set configuration
    |val config = ConfigFactory.load()
    |val sparkConfig = Try(config.getConfig("spark"))
    |  .getOrElse(com.typesafe.config.ConfigFactory.empty)
    |  .atPath("spark").entrySet.asScala.map(e => e.getKey -> config.getString(e.getKey))
    |
    |sparkConf.setAll(sparkConfig)
    |
    |sparkConf.setMaster(sparkConf.get("spark.master", "local[*]"))
    |sparkConf.set("spark.app.name", sparkConf.get("spark.app.name", "${notebookName}"))
    |
    |// Distribute the jars to executors, so this do not require a separate spark installation
    |// This is needed only if not using spark-submit (comment otherwise)
    |${executorJarsCode(downloadedJars)}
    |setExecutorJars()
    |
    |// Create Spark Session / Spark Context
    |val sparkSession = SparkSession.builder().config(sparkConf).getOrCreate
    |val sparkContext = sparkSession.sparkContext
    |println("SparkConf used:" + sparkContext.getConf.toDebugString)
    |
    |// aliases
    |val sc = sparkContext
    |val ss = sparkSession
    |import ss.implicits._
    |
    |""".stripMargin
  }

  def loadCustomVars() : String = {
    val customVars = snb.metadata.flatMap(_.customVars)
      .map { vars =>
        val customVars = vars.map { case (k, v) => s"""  val $k = customVars.getString("$k") """ }.mkString("// custom variables\n", "\n", "")
        s"""
           |val customVars = config.getConfig("$NotebookVarsPrefix")
           |$customVars
           |
     """.stripMargin
      }
    customVars.getOrElse("// no custom variables ")
  }

  val stopContext = """|
    |sparkContext.stop
    |""".stripMargin

  val isClassDefinition: String => Boolean = str => str.split("\n").exists { line =>
    val trimmed = line.trim
    trimmed.startsWith("class") || trimmed.startsWith("case class")
  }

  implicit class CodeCellImplicits(cell: CodeCell){
    def isInterpretedCell: Boolean = cell.source.trim.startsWith(":")
    def isRegularCodeCell: Boolean = {
      Seq(None, Some("scala")).contains(cell.language) &&
        // simply ignore fancy interpreted cells for now
        !cell.isInterpretedCell
    }
  }

  def buildMainCode():String = {
    val imports = snb.metadata.get.customImports.getOrElse(Nil).mkString("\n")
    val uuid = snb.metadata.map(_.id).getOrElse("id-not-found")
    val customVars = loadCustomVars()
    val code = snb.cells.map { cells =>
      val cs = cells.collect {
        case cell : notebook.NBSerializer.CodeCell if cell.isRegularCodeCell && !isClassDefinition(cell.source)=>
            cell.metadata.id → cell.source
      }
      val code = cs.map{ case (id, code) => id → code.split("\n").map { s => s"  $s" }.mkString("\n") }
                  .map{ case (id, code) => (s"\n\n  /* -- Code Cell: ${id} -- */ \n\n$code") }
                  .mkString("\n/****************/\n").trim
      code
    }.getOrElse("//NO CELLS!")


    List(imports, customVars, code, stopContext).mkString("\n")
  }

  def buildClasses() : String = {
      val imports = snb.metadata.get.customImports.getOrElse(Nil).mkString("\n")
      val code = snb.cells.map { cells =>
        val cs = cells.collect {
          case cell : notebook.NBSerializer.CodeCell if cell.isRegularCodeCell && isClassDefinition(cell.source)=>
            cell.metadata.id → cell.source
        }
        val code = cs.map{ case (id, code) => (id, code.split("\n").map {s => "  " + s}.mkString("\n")) }
          .map{ case (id, code) => (s"\n\n  /* -- Code Cell: ${id} -- */ \n\n$code") }
          .mkString("\n/****************/\n").trim
        code
      }.getOrElse("//NO CELLS!")
      val separator = "  //---//"
      List(imports, separator, code).mkString("\n")
  }

  def mockedChartsCode : String = {
    """
      |package notebook.front.widgets.charts
      |
      |import notebook.front.widgets.charts.MockedCharts.DEFAULT_MAX_POINTS
      |
      |object MockedCharts {
      |  val DEFAULT_MAX_POINTS = 1000
      |
      |  def display[C](originalData:C, fields:Option[(String, String)]=None, maxPoints:Int=0) = {}
      |  def pairs[C](originalData:C, maxPoints:Int=0) = {}
      |  def ul(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None) = {}
      |  def ol(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None) = {}
      |  def img(tpe:String="png", width:String="", height:String="") = {}
      |  def text(value: String) = {}
      |}
      |
      |case class CustomC3Chart[C](
      |                             originalData: C,
      |                             chartOptions: String = "{}",
      |                             sizes: (Int, Int) = (600, 400),
      |                             maxPoints: Int = DEFAULT_MAX_POINTS
      |                           )
      |
      |case class ScatterChart[C](
      |                            originalData: C,
      |                            fields: Option[(String, String)] = None,
      |                            sizes: (Int, Int) = (600, 400),
      |                            maxPoints: Int = DEFAULT_MAX_POINTS,
      |                            groupField: Option[String] = None
      |                          )
      |
      |case class LineChart[C](
      |                         originalData: C,
      |                         fields: Option[(String, String)] = None,
      |                         sizes: (Int, Int) = (600, 400),
      |                         maxPoints: Int = DEFAULT_MAX_POINTS,
      |                         groupField: Option[String] = None
      |                       )
      |
      |case class RadarChart[C](
      |                          originalData: C,
      |                          labelField: Option[String] = None,
      |                          sizes: (Int, Int) = (600, 400),
      |                          maxPoints: Int = DEFAULT_MAX_POINTS
      |                        )
      |
      |
      |case class ParallelCoordChart[C](
      |                                  originalData: C,
      |                                  sizes: (Int, Int) = (600, 400),
      |                                  maxPoints: Int = DEFAULT_MAX_POINTS
      |                                )
      |
      |case class TimeseriesChart[C](
      |                               originalData: C,
      |                               fields: Option[(String, String)] = None,
      |                               sizes: (Int, Int) = (600, 400),
      |                               maxPoints: Int = DEFAULT_MAX_POINTS,
      |                               groupField: Option[String] = None,
      |                               tickFormat: String = "%Y-%m-%d %H:%M:%S"
      |                             )
      |
      |case class BarChart[C](
      |                        originalData: C,
      |                        fields: Option[(String, String)] = None,
      |                        sizes: (Int, Int) = (600, 400),
      |                        maxPoints: Int = DEFAULT_MAX_POINTS,
      |                        groupField: Option[String] = None
      |                      )
      |
      |
      |case class PieChart[C](
      |                        originalData: C,
      |                        fields: Option[(String, String)] = None,
      |                        sizes: (Int, Int) = (600, 400),
      |                        maxPoints: Int = DEFAULT_MAX_POINTS
      |                      )
      |
      |case class DiyChart[C](
      |                        originalData: C,
      |                        js: String = "",
      |                        sizes: (Int, Int) = (600, 400),
      |                        maxPoints: Int = DEFAULT_MAX_POINTS
      |                      )
      |
      |
      |case class GeoPointsChart[C](
      |                              originalData: C,
      |                              sizes: (Int, Int) = (600, 400),
      |                              maxPoints: Int = DEFAULT_MAX_POINTS,
      |                              latLonFields: Option[(String, String)] = None,
      |                              rField: Option[String] = None,
      |                              colorField: Option[String] = None
      |                            )
      |
      |
      |case class GeoChart[C](
      |                        originalData: C,
      |                        sizes: (Int, Int) = (600, 400),
      |                        maxPoints: Int = DEFAULT_MAX_POINTS,
      |                        geometryField: Option[String] = None,
      |                        rField: Option[String] = None,
      |                        colorField: Option[String] = None,
      |                        fillColorField: Option[String] = None
      |                      )
      |
      |case class GraphChart[C](
      |                          originalData: C,
      |                          sizes: (Int, Int) = (600, 400),
      |                          maxPoints: Int = DEFAULT_MAX_POINTS,
      |                          charge: Int = -30,
      |                          linkDistance: Int = 20,
      |                          linkStrength: Double = 1.0
      |                        )
      |
      |
      |case class PivotChart[C](
      |                          originalData: C,
      |                          sizes: (Int, Int) = (600, 400),
      |                          maxPoints: Int = DEFAULT_MAX_POINTS,
      |                          // FIXME: otherwise this would add dependency on play-json!
      |                          // derivedAttributes:JsObject=play.api.libs.json.Json.obj(),
      |                          options: Map[String, String] = Map.empty
      |                        )
      |
      |
      |case class CustomPlotlyChart[C](
      |                                 originalData: C,
      |                                 layout: String = "{}",
      |                                 dataOptions: String = "{}",
      |                                 dataSources: String = "{}",
      |                                 sizes: (Int, Int) = (600, 400),
      |                                 maxPoints: Int = DEFAULT_MAX_POINTS
      |                               )
      |
      |case class TableChart[C](
      |                          originalData: C,
      |                          filterCol: Option[Seq[String]] = None,
      |                          sizes: (Int, Int) = (600, 400),
      |                          maxPoints: Int = DEFAULT_MAX_POINTS
      |                        )
      |
    """.stripMargin
  }
}

object Job {
  val NotebookVarsPrefix = "notebook.custom.vars"
  object Build {
    val properties = s"""
      |
      |sbt.version=${Sbt.version}
      |
      |""".stripMargin.trim

    object Sbt {
      val version = "0.13.9"
      val plugins =  s"""
        |addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.5")
        |
        |addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.6")
        |
        |addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
        |""".stripMargin.trim
    }
  }
}
