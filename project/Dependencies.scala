import sbt._
import Keys._

import com.typesafe.sbt.SbtNativePackager._, Keys._

object Dependencies {

  val playDeps                = Seq(
                                    "com.typesafe.play"         %%            "play"              %      "2.3.7"                     excludeAll(ExclusionRule("com.typesafe.akka"), ExclusionRule("com.google.guava")),
                                    "com.typesafe.play"         %%            "play-test"         %      "2.3.7"        % "test"     excludeAll(ExclusionRule("com.typesafe.akka"), ExclusionRule("com.google.guava"))
                                )
  val rxScala                 = "io.reactivex"              %%           "rxscala"            %      "0.22.0"

  val scalaZ                  = "org.scalaz"                %%         "scalaz-core"          %      "7.0.6"

  val akkaVersion             = "2.3.4-spark"
  val akka                    = "org.spark-project.akka"    %%         "akka-actor"           %    akkaVersion
  val akkaRemote              = "org.spark-project.akka"    %%         "akka-remote"          %    akkaVersion
  val akkaSlf4j               = "org.spark-project.akka"    %%          "akka-slf4j"          %    akkaVersion


  val defaultScalaVersion     = sys.props.getOrElse("scala.version", "2.10.4")
  val breeze                  = "org.scalanlp"              %%         "breeze"               %       "0.10"        excludeAll(ExclusionRule("junit"), ExclusionRule("org.apache.commons", "commons-math3"))
  val defaultSparkVersion     = sys.props.getOrElse("spark.version", "1.4.0")
  def sparkCore(v:String)     = "org.apache.spark"          %%         "spark-core"           %         v           excludeAll(ExclusionRule("org.apache.hadoop"), ExclusionRule("org.apache.ivy", "ivy"))
  def sparkYarn(v:String)     =
    if (v == "1.2.0") {
      "org.apache.spark"          %%         "spark-yarn"           %         (v + "-adatao") excludeAll(ExclusionRule("org.apache.hadoop"), ExclusionRule("org.apache.ivy", "ivy"))
    } else {
      "org.apache.spark"          %%         "spark-yarn"           %         v              excludeAll(ExclusionRule("org.apache.hadoop"), ExclusionRule("org.apache.ivy", "ivy"))
    }
  val defaultWithHive         = sys.props.getOrElse("with.hive", "false").toBoolean
  def sparkHive(v:String)     = "org.apache.spark"          %%         "spark-hive"           %         v           excludeAll(ExclusionRule("org.apache.hadoop"), ExclusionRule("org.apache.ivy", "ivy"), ExclusionRule("com.twitter", "parquet-column"), ExclusionRule("com.twitter", "parquet-hadoop"))
  def sparkRepl(v:String)     = "org.apache.spark"          %%         "spark-repl"           %         v           excludeAll(ExclusionRule("org.apache.hadoop"))
  def sparkSQL (v:String)     = "org.apache.spark"          %%         "spark-sql"            %         v           excludeAll(ExclusionRule("org.apache.hadoop"), ExclusionRule("com.twitter", "parquet-column"), ExclusionRule("com.twitter", "parquet-hadoop"))
  val defaultHadoopVersion    = sys.props.getOrElse("hadoop.version", "1.0.4")
  def hadoopClient(v:String)  = "org.apache.hadoop"         %         "hadoop-client"         %         v           excludeAll(ExclusionRule("org.apache.commons", "commons-exec"), ExclusionRule("commons-codec", "commons-codec"))
  val defaultJets3tVersion    = sys.props.getOrElse("jets3t.version", "0.7.1")
  def jets3t(v:String)        = "net.java.dev.jets3t"       %            "jets3t"             %         v           force()

  val commonsIO               = "org.apache.commons"        %          "commons-io"           %      "1.3.2"
  val commonsHttp             = "org.apache.httpcomponents" %          "httpclient"           %      "4.3.4"       excludeAll(ExclusionRule("com.google.guava"))
  val commonsExec             = "org.apache.commons"        %          "commons-exec"         %    "[1.2, 1.2]"    force()
  val commonsCodec            = "commons-codec"             %          "commons-codec"         %      "1.10"       force()

  val guava                   = "com.google.guava"          %            "guava"              %     "14.0.1"       force()

  val slf4jLog4j              = "org.slf4j"                 %         "slf4j-log4j12"         %      "1.7.7"
  val log4j                   = "log4j"                     %             "log4j"             %      "1.2.17"

  // to download deps at runtime
  def depsToDownloadDeps(scalaBinaryVersion:String, sbtVersion:String)    =  scalaBinaryVersion match {
    case "2.10" =>  List(
                      "org.scala-sbt"             %             "sbt"                         %     sbtVersion    excludeAll((ExclusionRule("org.apache.ivy", "ivy"))),
                       ("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.5.0") // WARN ONLY 2.10 0.13 available !!!!
                        .extra( CustomPomParser.SbtVersionKey -> sbtVersion.reverse.dropWhile(_ != '.').drop(".".size).reverse,
                                CustomPomParser.ScalaVersionKey -> scalaBinaryVersion)
                        .copy(crossVersion = CrossVersion.Disabled)
                        .excludeAll(ExclusionRule("org.apache.ivy", "ivy"))
                    )
    case _ =>
       val aetherApi               = "org.sonatype.aether"       %          "aether-api"           %     "1.13"
       val jcabiAether             = "com.jcabi"                 %         "jcabi-aether"          %     "0.10.1"
       val mavenCore               = "org.apache.maven"          %          "maven-core"           %     "3.0.5"
       List(aetherApi, jcabiAether, mavenCore)
  }
  // for aether only
  val ningAsyncHttpClient     = "com.ning"                  %       "async-http-client"       % "[1.6.5, 1.6.5]"   force()//"1.8.10"//"[1.6.5, 1.6.5]" force()

  // Viz
  val bokeh                   = "io.continuum.bokeh"        %%            "bokeh"             %       "0.2"
  val wisp                    = "com.quantifind"            %%            "wisp"              %      "0.0.2" excludeAll(ExclusionRule("com.google.guava"))
  // wisp deps on jackson-module-scala_2.10 v2.4 → guava v15
  // but spark 1.2 → guava 14.0.1
  val customJacksonScala      = Seq(
                                  "com.fasterxml.jackson.module"   %%  "jackson-module-scala"     %    "2.3.3" force()  excludeAll(ExclusionRule("com.google.guava")),
                                  "com.fasterxml.jackson.core"     % "jackson-annotations"        %    "2.3.3" force()  excludeAll(ExclusionRule("com.google.guava")),
                                  "com.fasterxml.jackson.module"   % "jackson-module-jsonSchema"  %    "2.3.3" force()  excludeAll(ExclusionRule("com.google.guava")),
                                  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda"      %    "2.3.3" force()  excludeAll(ExclusionRule("com.google.guava"))
                                )

  // FIXME
  val sparkEnabledModules = List("common", "kernel", "subprocess")


  object SparkVersion extends Enumeration {
    type SparkVersion = Value
    val `1.2.0`, `1.2.1`, `1.2.2`, `1.3.0`, `1.3.1` = Value
  }

  object HadoopVersion extends Enumeration {
    type HadoopVersion = Value
    val `1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2` = Value
  }

  val crossConf = Map(
    SparkVersion.`1.2.0` → { import HadoopVersion._; List(`1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2`) },
    SparkVersion.`1.2.1` → { import HadoopVersion._; List(`1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2`) },
    SparkVersion.`1.2.2` → { import HadoopVersion._; List(`1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2`) },
    SparkVersion.`1.3.0` → { import HadoopVersion._; List(`1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2`) },
    SparkVersion.`1.3.1` → { import HadoopVersion._; List(`1.0.4`, `2.0.0-cdh4.2.0`, `2.2.0`, `2.3.0`, `2.4.0`, `2.5.0`, `2.6.0`, `2.5.0-cdh5.3.1`, `2.5.0-cdh5.3.2`) }
  )

  val extraConf:Map[(SparkVersion.Value, HadoopVersion.Value), List[sbt.Def.Setting[_]]] = Map(
    (SparkVersion.`1.2.0`, List(HadoopVersion.`2.3.0`, HadoopVersion.`2.4.0`, HadoopVersion.`2.5.0`, HadoopVersion.`2.6.0`, HadoopVersion.`2.5.0-cdh5.3.1`, HadoopVersion.`2.5.0-cdh5.3.2`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0"
      )
    },
    (SparkVersion.`1.2.1`, List(HadoopVersion.`2.3.0`, HadoopVersion.`2.4.0`, HadoopVersion.`2.5.0`, HadoopVersion.`2.6.0`, HadoopVersion.`2.5.0-cdh5.3.1`, HadoopVersion.`2.5.0-cdh5.3.2`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0",
        Shared.jets3tVersion in "tachyon" := "0.9.0"
      )
    },
    (SparkVersion.`1.2.2`, List(HadoopVersion.`2.3.0`, HadoopVersion.`2.4.0`, HadoopVersion.`2.5.0`, HadoopVersion.`2.6.0`, HadoopVersion.`2.5.0-cdh5.3.1`, HadoopVersion.`2.5.0-cdh5.3.2`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0"
      )
    },
    (SparkVersion.`1.3.0`, List(HadoopVersion.`2.3.0`, HadoopVersion.`2.4.0`, HadoopVersion.`2.5.0`, HadoopVersion.`2.6.0`, HadoopVersion.`2.5.0-cdh5.3.1`, HadoopVersion.`2.5.0-cdh5.3.2`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0",
        Shared.jets3tVersion in "tachyon" := "0.9.0"
      )
    },
    (SparkVersion.`1.3.0`, List(HadoopVersion.`2.3.0`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0",
        Shared.jets3tVersion in "tachyon" := "0.9.0"
      )
    },
    (SparkVersion.`1.3.1`, List(HadoopVersion.`2.3.0`, HadoopVersion.`2.4.0`, HadoopVersion.`2.5.0`, HadoopVersion.`2.6.0`, HadoopVersion.`2.5.0-cdh5.3.1`, HadoopVersion.`2.5.0-cdh5.3.2`)) → {
      List(
        Shared.jets3tVersion := "0.9.0",
        Shared.jets3tVersion in "common" := "0.9.0",
        Shared.jets3tVersion in "kernel" := "0.9.0",
        Shared.jets3tVersion in "subprocess" := "0.9.0",
        Shared.jets3tVersion in "spark" := "0.9.0"
      )
    }
  ).flatMap { case ((s,hs), sg) =>
    hs.map(h => (s, h) → sg )
  }.toMap

  def extractEnumVersionName(s:String):String = s.toString.replaceAll("\\$u002E", ".").replaceAll("\\$minus", "-")

  def forAll[T](name:String, logS:String=>String, logH:String=>String, ts:List[sbt.TaskKey[_]]) = {
    Command.command(name) { state =>
      val extracted = Project.extract(state)
      val scalaActualVersion = extracted.get(scalaVersion)
      println(s"Running command $name using scala " + scalaActualVersion)
      crossConf
      .filter { case (sv, hvs) =>
        val svS = extractEnumVersionName(sv.toString)
        if (scalaActualVersion.startsWith("2.11") && svS == "1.2.0") {
          println("Skipping build for scala " + scalaActualVersion + " and spark " + "1.2.0. Because REPL has been built for it.")
          false
        } else {
          true
        }
      }
      .foreach { case (sv, hvs) =>
        val svS = extractEnumVersionName(sv.toString)
        println(logS(svS))
        hvs foreach { hv =>
          val hvS = extractEnumVersionName(hv.toString)
          println(logH(hvS))

          val shSettings = List(
            Shared.sparkVersion := svS,
            Shared.sparkVersion in "common" := svS,
            Shared.sparkVersion in "kernel" := svS,
            Shared.sparkVersion in "subprocess" := svS,
            Shared.sparkVersion in "spark" := svS,
            Shared.sparkVersion in "tachyon" := svS,

            Shared.hadoopVersion := hvS,
            Shared.hadoopVersion in "common" := hvS,
            Shared.hadoopVersion in "kernel" := hvS,
            Shared.hadoopVersion in "subprocess" := hvS,
            Shared.hadoopVersion in "spark" := hvS,
            Shared.hadoopVersion in "tachyon" := hvS
          )

          val extraSettings = extraConf.get((sv, hv)).getOrElse(Nil)

          ts.foreach { t =>
            println(s"Running task $t")
            Project.runTask(
              t,
              extracted.append(
                shSettings ::: extraSettings,
                state
              ),
              true
            )
          }
        }
      }

      state
    }
  }


  def distAll = forAll(
    "distAll",
    logS = sv => s" > Building: dist for Spark $sv",
    logH = hv => s"   > with hadoop $hv",
    (packageBin in Universal) :: (packageBin in Debian) :: Nil
  )

  def distZips = forAll(
    "distZips",
    logS = sv => s" > Building: dist for Spark $sv",
    logH = hv => s"   > with hadoop $hv",
    (packageBin in Universal) :: Nil
  )

  def distDebs = forAll(
    "distDebs",
    logS = sv => s" > Building: dist for Spark $sv",
    logH = hv => s"   > with hadoop $hv",
    (packageBin in Debian) :: Nil
  )

  def dockerPublishLocalAll = forAll(
    "dockerPublishLocalAll",
    logS = sv => s" > Publishing local: docker for Spark $sv",
    logH = hv => s"   > with hadoop $hv",
    (publishLocal in Docker) :: Nil
  )

  def dockerPublishAll = forAll(
    "dockerPublishAll",
    logS = sv => s" > Publishing remote: docker for Spark $sv",
    logH = hv => s"   > with hadoop $hv",
    (publish in Docker) :: Nil
  )

}