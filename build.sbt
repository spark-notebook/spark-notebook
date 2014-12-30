import Dependencies._

play.Project.playScalaSettings

organization := "noootsab"

name := "spark-notebook"

version := "0.1.2"

maintainer := "Andy Petrella" //Docker

dockerExposedPorts in Docker := Seq(9000, 9443) //Docker

dockerRepository := Some("andypetrella") //Docker

packageName in Docker := "andypetrella/spark-notebook"

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

compileOrder := CompileOrder.Mixed

publishMavenStyle := false

javacOptions ++= Seq("-Xlint:deprecation", "-g")

scalacOptions += "-deprecation"

scalacOptions ++= Seq("-Xmax-classfile-name", "100")

dependencyOverrides += "log4j" % "log4j" % "1.2.16"

libraryDependencies ++= Seq(
  playDep,
  akka,
  akkaRemote,
  akkaSlf4j,
  jdbc,
  anorm,
  cache,
  commonsIO,
  ningAsyncHttpClient, // for aether to work...
  "org.scala-lang" % "scala-library" % "2.10.4",
  "org.scala-lang" % "scala-reflect" % "2.10.4",
  "org.scala-lang" % "scala-compiler" % "2.10.4"
)

lazy val sparkNotebook = project.in(file("."))
    .aggregate(subprocess, observable, common, kernel)
    .dependsOn(subprocess, observable, common, kernel)
    .settings(
      scalaVersion := "2.10.4"
    )

lazy val subprocess =  project.in(file("modules/subprocess"))
                              .settings(
                                libraryDependencies ++= {
                                  Seq(
                                    playDep,
                                    akka,
                                    akkaRemote,
                                    akkaSlf4j,
                                    guava,
                                    sparkRepl,
                                    sparkSQL,
                                    hadoopClient,
                                    commonsIO,
                                    commonsExec,
                                    log4j
                                  )
                                }
                              )
                              .settings(
                                scalaVersion := "2.10.4"
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
                                scalaVersion := "2.10.4"
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
                                  guava,
                                  sparkRepl,
                                  sparkSQL,
                                  hadoopClient
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
                              .settings(
                                scalaVersion := "2.10.4"
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
                                ),
                                libraryDependencies ++= Seq(
                                  guava,
                                  sparkRepl,
                                  sparkSQL,
                                  hadoopClient
                                )
                              )
                              .settings(
                                scalaVersion := "2.10.4"
                              )
