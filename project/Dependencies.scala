import sbt._
import Keys._

object Dependencies {
  val akkaVersion          = "2.2.3-shaded-protobuf"

  val playDep              = "com.typesafe.play"         %%            "play"             %      "2.2.6"        excludeAll(ExclusionRule("com.typesafe.akka"))

  val akka                 = "org.spark-project.akka"    %%         "akka-actor"          %    akkaVersion
  val akkaRemote           = "org.spark-project.akka"    %%         "akka-remote"         %    akkaVersion
  val akkaSlf4j            = "org.spark-project.akka"    %%          "akka-slf4j"         %    akkaVersion
  val akkaTestkit          = "org.spark-project.akka"    %%         "akka-testkit"        %    akkaVersion    % "test"

  val rxScala              = "io.reactivex"              %%           "rxscala"           %      "0.22.0"

  val scalaZ               = "org.scalaz"                %%         "scalaz-core"         %      "7.0.6"

  val sparkRepl            = "org.apache.spark"          %%         "spark-repl"          %      "1.1.0"      //% "provided"
  val sparkSQL             = "org.apache.spark"          %%         "spark-sql"           %      "1.1.0"      //% "provided"

  val commonsIO            = "org.apache.commons"        %          "commons-io"          %      "1.3.2"
  val commonsHttp          = "org.apache.httpcomponents" %          "httpclient"          %      "4.3.4"
  val commonsExec          = "org.apache.commons"        %          "commons-exec"        %       "1.2"

  val guava                = "com.google.guava"          %            "guava"             %     "14.0.1"

  val slf4jLog4j           = "org.slf4j"                 %         "slf4j-log4j12"        %      "1.7.7"
  val log4j                = "log4j"                     %             "log4j"            %      "1.2.17"

  val scalaTest            = "org.scalatest"             %%          "scalatest"          %      "2.2.0"      % "test"
  val scalaMock            = "org.scalamock"             %% "scalamock-scalatest-support" %     "3.1.RC1"     % "test"

  // to download deps at runtime
  val aetherApi            = "org.sonatype.aether"       %          "aether-api"          %     "1.13.1"
  val jcabiAether          = "com.jcabi"                 %         "jcabi-aether"         %      "0.10"
  val mavenCore            = "org.apache.maven"          %          "maven-core"          %     "3.0.5"

  // Viz
  val bokeh                = "io.continuum.bokeh"        %          "bokeh_2.10"               %       "0.2"

}