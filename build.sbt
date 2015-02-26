import Dependencies._

import Shared._

//play.Project.playScalaSettings

organization := "noootsab"

name := "spark-notebook"

version in ThisBuild <<= (sparkVersion, hadoopVersion) { (sv, hv) => s"0.3.0-spark-$sv-hadoop-$hv" }

maintainer := "Andy Petrella" //Docker

dockerExposedPorts in Docker := Seq(9000, 9443) //Docker

dockerRepository := Some("andypetrella") //Docker

packageName in Docker := "spark-notebook"

scalaVersion := "2.10.4"

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

parallelExecution in Test in ThisBuild := false

// these java options are for the forked test JVMs
javaOptions in ThisBuild ++= Seq("-Xmx512M", "-XX:MaxPermSize=128M")

resolvers in ThisBuild ++=  Seq(
                              Resolver.typesafeRepo("releases"),
                              Resolver.sonatypeRepo("releases"),
                              Resolver.typesafeIvyRepo("releases"),
                              Resolver.typesafeIvyRepo("snapshots"),
                              "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos"
                            )

EclipseKeys.skipParents in ThisBuild := false
							
compileOrder := CompileOrder.Mixed

publishMavenStyle := false

javacOptions ++= Seq("-Xlint:deprecation", "-g")

scalacOptions += "-deprecation"

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

commands ++= Seq( distAll, dockerPublishLocalAll, dockerPublishAll )

dependencyOverrides += "log4j" % "log4j" % "1.2.16"

dependencyOverrides += guava

enablePlugins(DebianPlugin)

sharedSettings

libraryDependencies ++= playDeps

libraryDependencies ++= Seq(
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
  ningAsyncHttpClient, // for aether to work...
  "org.scala-lang" % "scala-library" % "2.10.4",
  "org.scala-lang" % "scala-reflect" % "2.10.4",
  "org.scala-lang" % "scala-compiler" % "2.10.4"
)

lazy val sparkNotebook = project.in(file(".")).enablePlugins(play.PlayScala).enablePlugins(SbtWeb)
    .aggregate(subprocess, observable, common, kernel)
    .dependsOn(subprocess, observable, common, kernel)
    .settings(
      sharedSettings:_*
    ).settings(
      includeFilter in (Assets, LessKeys.less) := "*.less"
    )


lazy val subprocess =  project.in(file("modules/subprocess"))
                              .settings(
                                libraryDependencies ++= playDeps
                              )
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
                              .settings(
                                sharedSettings:_*
                              )
                              .settings(
                                sparkSettings:_*
                              )


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
                              .settings(
                                sharedSettings:_*
                              )

lazy val common = Project(id = "common", base = file("modules/common"))
                              .dependsOn(observable)
                              .settings(
                                libraryDependencies ++= Seq(
                                  akka,
                                  log4j,
                                  scalaZ
                                ),
                                libraryDependencies ++= Seq(
                                  aetherApi,
                                  jcabiAether,
                                  mavenCore
                                ),
                                // plotting functionality
                                libraryDependencies ++= Seq(
                                  bokeh,
                                  wisp
                                )// ++ customJacksonScala
                              )
                              .settings(
                                sharedSettings:_*
                              )
                              .settings(
                                sparkSettings:_*
                              )

lazy val kernel = Project(id = "kernel", base = file("modules/kernel"))
                              .dependsOn(common, subprocess, observable)
                              .settings(
                                libraryDependencies ++= Seq(
                                  akkaRemote,
                                  akkaSlf4j,
                                  slf4jLog4j,
                                  commonsIO
                                ),
                                libraryDependencies ++= Seq(
                                  "org.scala-lang" % "jline" % scalaVersion.value,
                                  "org.scala-lang" % "scala-compiler" % scalaVersion.value
                                )
                              )
                              .settings(
                                sharedSettings:_*
                              )
                              .settings(
                                sparkSettings:_*
                              )




