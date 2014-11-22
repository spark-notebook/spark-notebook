/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */
import org.apache.ivy.core.module.id.ModuleRevisionId
import sbt._
import Keys._
import org.apache.ivy.core.install.InstallOptions
import com.untyped.sbtjs.Plugin._

import sbtassembly.Plugin._
import AssemblyKeys._


object NotebookBuild extends Build {

  implicit def toRichProject(project: Project) = new RichProject(project)
  import Dependencies._

  override val settings =  {
    super.settings ++ Seq(
      organization := "nooostab",
      version := "0.1.0-SNAPSHOT",
      scalaVersion in ThisBuild := "2.10.4",
      fork in Test in ThisBuild := true,
      parallelExecution in Test in ThisBuild := false,
      // these java options are for the forked test JVMs
      javaOptions in ThisBuild ++= Seq("-Xmx512M", "-XX:MaxPermSize=128M"),

      resolvers in ThisBuild ++= Seq(
        Resolver.typesafeRepo("releases"),
        Resolver.sonatypeRepo("releases"),
        Resolver.typesafeIvyRepo("releases"),
        Resolver.typesafeIvyRepo("snapshots")
      ),

      compileOrder := CompileOrder.Mixed,
      publishMavenStyle := false,
      javacOptions ++= Seq("-Xlint:deprecation", "-g"),
      scalacOptions += "-deprecation",
      scalacOptions ++= Seq("-Xmax-classfile-name", "100") ,
      testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v") //Suppress test output unless there is a failure
    )
  }

  lazy val root = Project(id = "spark-notebook", base = file("."), settings = Defaults.defaultSettings ++ assemblySettings)
    .aggregate(subprocess, observable, common, kernel, server)
    .dependsOn(subprocess, observable, common, kernel, server)
    .settings(
      publish := {}, // don't publish an empty jar for the root project
      publishLocal := {}
    )
    .settings(
      mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
        {
          case x if x.endsWith("MANIFEST.MF")                                       => MergeStrategy.discard
          case PathList("META-INF", "THIRD-PARTY.txt", xs @ _*)                     => MergeStrategy.concat
          case PathList("META-INF", "io.netty.versions.properties", xs @ _*)        => MergeStrategy.first
          case PathList("META-INF", "jboss-beans.xml", xs @ _*)                     => MergeStrategy.first
          case PathList("META-INF", "native", _, x, xs @ _*) if x.contains("jansi") => MergeStrategy.first
          case "mime.types"                                                         => MergeStrategy.filterDistinctLines
          case path if path.endsWith(".class")                                      => MergeStrategy.first
          case path if path.endsWith("beans.xml")                                   => MergeStrategy.first
          case x                                                                    => old(x)
        }
      }
    )

  lazy val subprocess = Project(id = "subprocess", base = file("subprocess"))
    .projectDefaults
    .settings(
      libraryDependencies ++= Seq(
        akkaRemote,
        akkaSlf4j,
        akkaTestkit,
        slf4jLog4j,
        scalaTest,
        commonsIO,
        "org.apache.commons" % "commons-exec" % "1.2"
      )
    )

  lazy val observable = Project(id = "observable", base = file("observable"))
    .dependsOn(subprocess)
    .projectDefaults
    .withWebAssets
    .settings(
      libraryDependencies ++= Seq(
        akkaRemote,
        akkaSlf4j,
        akkaTestkit,
        slf4jLog4j,
        unfilteredFilter,
        unfilteredWebsockets,
        unfilteredJson,
        scalaTest,
        "io.reactivex" %% "rxscala" % "0.22.0"
      )
    )

  lazy val common = Project(id = "common", base = file("common"))
    .dependsOn(observable)
    .projectDefaults
    .settings(
      name := "notebook-common",

      libraryDependencies ++= Seq(
        akka,
        unfilteredJson,
        scalaTest,
        log4j,
        "org.scalaz" %% "scalaz-core" % "7.0.6"
      ),

      libraryDependencies ++= Seq(
        sparkRepl,
        sparkSQL
      ),

      libraryDependencies ++= Seq(
        aetherApi,
        jcabiAether,
        mavenCore
      ),

      // plotting functionality
      libraryDependencies ++= Seq(
        bokeh
      )
    )

  lazy val kernel = Project(id = "kernel", base = file("kernel"))
    .dependsOn(common, subprocess, observable)
    .projectDefaults
    .settings(
      name := "notebook-kernel",

      libraryDependencies ++= Seq(
        akkaRemote,
        akkaSlf4j,
        akkaTestkit,
        slf4jLog4j,
        commonsIO,
        scalaTest
      ),

      libraryDependencies ++= Seq(
        "org.scala-lang" % "jline" % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value
      ),

      libraryDependencies ++= Seq(
        sparkRepl,
        sparkSQL
      )
    )

  lazy val server = Project(id = "server", base = file("server"))
    .dependsOn(common, kernel)
    .projectDefaults
    .withWebAssets
    .settings(
      name := "notebook-server",

      mainClass in (Compile, run) := Some("notebook.Server"),
      mainClass in assembly := Some("notebook.Server"),

      libraryDependencies ++= Seq(
        akkaRemote,
        akkaSlf4j,
        slf4jLog4j,
        unfilteredFilter,
        unfiltereNettyServer,
        unfilteredWebsockets,
        akkaTestkit,
        unfilteredJson,
        commonsIO,
        commonsHttp,
        scalaTest,
        scalaMock,
        "org.fusesource.scalate" %% "scalate-core" % "1.6.1"
      ),

      libraryDependencies ++= Seq(
        sparkRepl,
        sparkSQL
      ),

      libraryDependencies ++= Seq(
        bokeh
      ),

      run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
    )

  object Dependencies {
    val unfilteredVersion    = "0.8.2"
    val akkaVersion          = "2.2.3-shaded-protobuf"

    val sparkRepl            = "org.apache.spark"          %%         "spark-repl"          %      "1.1.0"      % "provided"
    val sparkSQL             = "org.apache.spark"          %%         "spark-sql"           %      "1.1.0"      % "provided"
    val commonsIO            = "org.apache.commons"        %          "commons-io"          %      "1.3.2"
    val commonsHttp          = "org.apache.httpcomponents" %          "httpclient"          %      "4.3.4"
    val slf4jLog4j           = "org.slf4j"                 %         "slf4j-log4j12"        %      "1.7.7"
    val log4j                = "log4j"                     %             "log4j"            %      "1.2.17"
    val unfilteredFilter     = "net.databinder"            %%      "unfiltered-filter"      % unfilteredVersion
    val unfiltereNettyServer = "net.databinder"            %%   "unfiltered-netty-server"   % unfilteredVersion
    val unfilteredWebsockets = "net.databinder"            %% "unfiltered-netty-websockets" % unfilteredVersion
    val unfilteredJson       = "net.databinder"            %%      "unfiltered-json4s"      % unfilteredVersion
    val akka                 = "org.spark-project.akka"    %%         "akka-actor"          %    akkaVersion
    val akkaRemote           = "org.spark-project.akka"    %%         "akka-remote"         %    akkaVersion
    val akkaSlf4j            = "org.spark-project.akka"    %%          "akka-slf4j"         %    akkaVersion
    val akkaTestkit          = "org.spark-project.akka"    %%         "akka-testkit"        %    akkaVersion    % "test"
    val scalaTest            = "org.scalatest"             %%          "scalatest"          %      "2.2.0"      % "test"
    val scalaMock            = "org.scalamock"             %% "scalamock-scalatest-support" %     "3.1.RC1"     % "test"

    // to download deps at runtime
    val aetherApi            = "org.sonatype.aether"       %          "aether-api"          %     "1.13.1"
    val jcabiAether          = "com.jcabi"                 %         "jcabi-aether"         %      "0.10"
    val mavenCore            = "org.apache.maven"          %          "maven-core"          %     "3.0.5"

    val bokeh                = "io.continuum.bokeh"        %          "bokeh_2.10"               %       "0.2"
  }


  class RichProject(project: Project)  {
    def projectDefaults = project.settings(
      resourceDirectories in Test <++= resourceDirectories in Compile
    )

    def withWebAssets = {
      project.settings(jsSettings : _*)
        .settings(
          (sourceDirectory in (Compile, JsKeys.js)) <<= (sourceDirectory in Compile)(_ / "assets"),
          (resourceGenerators in Compile) <+= (JsKeys.js in Compile),
          // Disable minification
          // TODO: make this conditional.  Ideally have minification off when running from SBT, on when packaging/publishing.
          // Might also be useful to publish debug binaries, maybe in an alternate config/classifier?
          (JsKeys.variableRenamingPolicy in (Compile, JsKeys.js)) := VariableRenamingPolicy.OFF,
          (JsKeys.prettyPrint in (Compile, JsKeys.js)) := true,

          // avoid injected 'require' function in scopedScript to be renamed, and thus not found at runtime
          (JsKeys.compilationLevel in (Compile, JsKeys.js)) := CompilationLevel.WHITESPACE_ONLY,

          /* Copy all non-compiled assets */
          unmanagedResourceDirectories in Compile <+= (sourceDirectory in Compile) (_ / "assets"),
          excludeFilter in (Compile, unmanagedResources) ~= (filter => filter || "*.js" || "*.coffee" || "*.jsm")
      )
    }
  }

}
