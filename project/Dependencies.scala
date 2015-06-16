import sbt._

object Dependencies {
  val mesosVersion = sys.props.getOrElse("mesos.version", "0.22.0") //0.22.0 is current DCOS version

  val playDeps = Seq(
    "com.typesafe.play" %% "play" % "2.3.7" withSources() excludeAll(
      ExclusionRule("com.typesafe.akka"),
      ExclusionRule("com.google.guava")
      ),
    "com.typesafe.play" %% "play-test" % "2.3.7" % "test" withSources() excludeAll(
      ExclusionRule("com.typesafe.akka"),
      ExclusionRule("com.google.guava")
      )
  )
  val rxScala = "io.reactivex" %% "rxscala" % "0.22.0"
  val scalaZ = "org.scalaz" %% "scalaz-core" % "7.0.6"

  val akkaVersion = "2.3.4-spark"
  val akka = "org.spark-project.akka" %% "akka-actor" % akkaVersion
  val akkaRemote = "org.spark-project.akka" %% "akka-remote" % akkaVersion
  val akkaSlf4j = "org.spark-project.akka" %% "akka-slf4j" % akkaVersion

  val defaultScalaVersion = sys.props.getOrElse("scala.version", "2.10.4")
  val breeze = "org.scalanlp" %% "breeze" % "0.10" excludeAll(
    ExclusionRule("junit"),
    ExclusionRule("org.apache.commons", "commons-math3")
    )
  val defaultSparkVersion = sys.props.getOrElse("spark.version", "1.4.0")

  def sparkCore(v: String) = "org.apache.spark" %% "spark-core" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.apache.ivy", "ivy")
    )

  def sparkYarn(v: String) = if (v == "1.2.0") {
    "org.apache.spark" %% "spark-yarn" % (v + "-adatao") excludeAll(
      ExclusionRule("org.apache.hadoop"),
      ExclusionRule("org.apache.ivy", "ivy")
      )
  } else {
    "org.apache.spark" %% "spark-yarn" % v excludeAll(
      ExclusionRule("org.apache.hadoop"),
      ExclusionRule("org.apache.ivy", "ivy")
      )
  }

  val defaultWithHive = sys.props.getOrElse("with.hive", "false").toBoolean
  val defaultWithParquet = sys.props.getOrElse("with.parquet", "false").toBoolean
  val parquetList:List[ExclusionRule] =
    if (!defaultWithParquet) {
      List(
        ExclusionRule("com.twitter", "parquet-column"),
        ExclusionRule("com.twitter", "parquet-hadoop")
      )
    } else {
      Nil
    }

  def sparkHive(v: String) = "org.apache.spark" %% "spark-hive" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.apache.ivy", "ivy")
  ) excludeAll(parquetList:_*)

  def sparkRepl(
    v: String) = "org.apache.spark" %% "spark-repl" % v excludeAll ExclusionRule("org.apache.hadoop")

  def sparkSQL(v: String) = "org.apache.spark" %% "spark-sql" % v excludeAll(
    ExclusionRule("org.apache.hadoop")
  ) excludeAll(parquetList:_*)

  val defaultHadoopVersion = sys.props.getOrElse("hadoop.version", "1.0.4")

  def hadoopClient(
    v: String) = "org.apache.hadoop" % "hadoop-client" % v excludeAll(
    ExclusionRule("org.apache.commons", "commons-exec"),
    ExclusionRule("commons-codec", "commons-codec"),
    ExclusionRule("com.google.guava", "guava")
    )

  val defaultJets3tVersion = sys.props.getOrElse("jets3t.version", "0.7.1")

  def jets3t(jv: Option[String],
    hv: Option[String]) = {
    val hvr = "([0-9])\\.([0-9]+)\\..+".r
    val v = (jv, hv) match {
      case (Some(x), _) => x
      case (_, Some(x)) => x match {
        case hvr("2", x) if x.toInt >= 3 => "0.9.0"
        case _ => defaultJets3tVersion
      }
    }
    "net.java.dev.jets3t" % "jets3t" % v force()
  }

  val commonsIO = "org.apache.commons" % "commons-io" % "1.3.2"
  val commonsHttp = "org.apache.httpcomponents" % "httpclient" % "4.3.4" excludeAll ExclusionRule("com.google.guava")
  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3" force()
  val commonsCodec = "commons-codec" % "commons-codec" % "1.10" force()
  val guava = "com.google.guava" % "guava" % "14.0.1" force()
  val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % "1.7.7"
  val log4j = "log4j" % "log4j" % "1.2.17"

  // to download deps at runtime
  def depsToDownloadDeps(scalaBinaryVersion: String,
    sbtVersion: String) = scalaBinaryVersion match {
    case "2.10" => List(
      "org.scala-sbt" % "sbt" % sbtVersion excludeAll ExclusionRule("org.apache.ivy", "ivy"),
      ("com.frugalmechanic" % "fm-sbt-s3-resolver" % "0.5.0") // WARN ONLY 2.10 0.13 available !!!!
        .extra(
          CustomPomParser.SbtVersionKey -> sbtVersion.reverse.dropWhile(_ != '.').drop(".".length).reverse,
          CustomPomParser.ScalaVersionKey -> scalaBinaryVersion
        )
        .copy(crossVersion = CrossVersion.Disabled)
        .excludeAll(ExclusionRule("org.apache.ivy", "ivy"))
    )
    case _ =>
      val aetherApi = "org.sonatype.aether" % "aether-api" % "1.13"
      val jcabiAether = "com.jcabi" % "jcabi-aether" % "0.10.1"
      val mavenCore = "org.apache.maven" % "maven-core" % "3.0.5"
      List(aetherApi, jcabiAether, mavenCore)
  }

  // for aether only
  val ningAsyncHttpClient = "com.ning" % "async-http-client" % "[1.6.5, 1.6.5]" force() //"1.8.10"//"[1.6.5, 1.6.5]" force()

  // Viz
  val bokeh = "io.continuum.bokeh" %% "bokeh" % "0.2"
  val wisp = "com.quantifind" %% "wisp" % "0.0.2" excludeAll ExclusionRule("com.google.guava")
  // wisp deps on jackson-module-scala_2.10 v2.4 → guava v15
  // but spark 1.2 → guava 14.0.1
  val customJacksonScala = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.3" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.3" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.3.3" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.3.3" force() excludeAll ExclusionRule("com.google.guava")
  )
}