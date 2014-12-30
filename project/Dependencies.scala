import sbt._
import Keys._

object Dependencies {

  val akkaVersion           = "2.2.3-shaded-protobuf"

  val playDep               = "com.typesafe.play"         %%            "play"              %      "2.2.6"        excludeAll(ExclusionRule("com.typesafe.akka"))

  val akka                  = "org.spark-project.akka"    %%         "akka-actor"           %    akkaVersion
  val akkaRemote            = "org.spark-project.akka"    %%         "akka-remote"          %    akkaVersion
  val akkaSlf4j             = "org.spark-project.akka"    %%          "akka-slf4j"          %    akkaVersion

  val rxScala               = "io.reactivex"              %%           "rxscala"            %      "0.22.0"

  val scalaZ                = "org.scalaz"                %%         "scalaz-core"          %      "7.0.6"

  val sparkVersion          = sys.props.getOrElse("spark.version", default = "1.1.0")
  val hadoopVersion         = sys.props.getOrElse("hadoop.version", default = "1.0.4")
  val sparkRepl             = "org.apache.spark"          %%         "spark-repl"           %    sparkVersion     excludeAll(ExclusionRule("org.apache.hadoop"))
  val sparkSQL              = "org.apache.spark"          %%         "spark-sql"            %    sparkVersion     excludeAll(ExclusionRule("org.apache.hadoop"))
  val hadoopClient          = "org.apache.hadoop"         %         "hadoop-client"         %    hadoopVersion

  val commonsIO             = "org.apache.commons"        %          "commons-io"           %      "1.3.2"
  val commonsHttp           = "org.apache.httpcomponents" %          "httpclient"           %      "4.3.4"
  val commonsExec           = "org.apache.commons"        %          "commons-exec"         %       "[1.2, 1.2]" force()

  val guava                 = "com.google.guava"          %            "guava"              %     "14.0.1"

  val slf4jLog4j            = "org.slf4j"                 %         "slf4j-log4j12"         %      "1.7.7"
  val log4j                 = "log4j"                     %             "log4j"             %      "1.2.17"

  // to download deps at runtime
  val aetherApi             = "org.sonatype.aether"       %          "aether-api"           %     "1.13"
  val jcabiAether           = "com.jcabi"                 %         "jcabi-aether"          %     "0.10.1"
  val mavenCore             = "org.apache.maven"          %          "maven-core"           %     "3.0.5"
  val ningAsyncHttpClient   = "com.ning"                  %       "async-http-client"       %     "[1.6.5, 1.6.5]" force()

  // Viz
  val bokeh                 = "io.continuum.bokeh"        %          "bokeh_2.10"           %       "0.2"

}