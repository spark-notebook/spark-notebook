import sbt._

object Dependencies {
  val mesosVersion = sys.props.getOrElse("mesos.version", "0.22.2") //0.22.0 is current DCOS version

  val playDeps = Seq(
    "com.typesafe.play" %% "play" % "2.3.10" withSources() excludeAll(
      ExclusionRule("com.typesafe.akka"),
      ExclusionRule("com.google.guava")
      ),
    "com.typesafe.play" %% "play-test" % "2.3.10" % "test" withSources() excludeAll(
      ExclusionRule("com.typesafe.akka"),
      ExclusionRule("com.google.guava")
      )
  )
  val rxScala = "io.reactivex" %% "rxscala" % "0.22.0"
  val scalaZ = "org.scalaz" %% "scalaz-core" % "7.0.6"

  val defaultHadoopVersion = sys.props.getOrElse("hadoop.version", "2.2.0")

  val akkaGroup = if (defaultHadoopVersion.startsWith("1")) "org.spark-project.akka" else "com.typesafe.akka"
  val akkaVersion = if (defaultHadoopVersion.startsWith("1")) "2.3.4-spark" else "2.3.11"
  val akka = akkaGroup %% "akka-actor" % akkaVersion
  val akkaRemote = akkaGroup %% "akka-remote" % akkaVersion
  val akkaSlf4j = akkaGroup %% "akka-slf4j" % akkaVersion

  val scala_2_1X = "2\\.1([0-9])\\.[0-9]+.*".r
  val spark_X_Y = "[a-zA-Z]*([0-9]+)\\.([0-9]+)\\.([0-9]+).*".r
  val extractVs = "[a-zA-Z]*(\\d+)\\.(\\d+)\\.(\\d+).*".r

  val defaultSparkVersion = sys.props.getOrElse("spark.version", "1.6.2")
  if (defaultSparkVersion.startsWith("2")) {
    scala.Console.err.println("""|
      |***************************************************************************************************
      |*                                                                                                 *
      |*                     This branch isn't able to build against spark 2+                            *
      |*                      Please use the `future-master` branch instead                              *
      |*                                                                                                 *
      |***************************************************************************************************
      """.stripMargin)
    sys.exit(1)
  }

  val sparkVersionTuple = defaultSparkVersion match { case extractVs(v, m, p) =>  (v.toInt, m.toInt, p.toInt)}
  val defaultScalaVersion = sys.props.getOrElse("scala.version", "2.10.5") match {
    case x@scala_2_1X("0") => defaultSparkVersion match {
      case spark_X_Y("1", x, _) if x.toInt < 6 => "2.10.4"
      case spark_X_Y("1", "6", _)              => "2.10.5"
      case spark_X_Y("2", _, _)                => "2.10.6"
      case spark_X_Y(_, _, _)                  => x
    }
    case x@scala_2_1X("1") => defaultSparkVersion match {
      case spark_X_Y("1", "4", "0") => x
      case spark_X_Y("1", "4", _) => "2.11.6"
      case spark_X_Y("1", "5", x) if x.toInt < 2 => "2.11.6"
      case spark_X_Y("1", "5", _) => "2.11.7"
      case spark_X_Y("1", "6", _) => "2.11.7"
      case spark_X_Y("2", _, _) => "2.11.8"
      case spark_X_Y(_, _, _) => x
    }
  }
  val breeze = "org.scalanlp" %% "breeze" % "0.10" excludeAll(
    ExclusionRule("junit"),
    ExclusionRule("org.apache.commons", "commons-math3")
  )

  def sparkCore(v: String) = "org.apache.spark" %% "spark-core" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.apache.ivy", "ivy")
  )

  def sparkYarn(v: String) = "org.apache.spark" %% "spark-yarn" % v excludeAll(
      ExclusionRule("org.apache.hadoop"),
      ExclusionRule("javax.servlet", "servlet-api"),
      ExclusionRule("org.mortbay.jetty", "servlet-api"),
      ExclusionRule("org.apache.ivy", "ivy")
    )

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
    ExclusionRule("org.apache.ivy", "ivy"),
    ExclusionRule("javax.servlet", "servlet-api"),
    ExclusionRule("org.mortbay.jetty", "servlet-api")
  ) excludeAll(parquetList:_*) excludeAll(
    {
      import scala.math.Ordering.Implicits._
      if (sparkVersionTuple >= (1, 5, 2)) {
        Nil
      } else {
        List(
          ExclusionRule("com.twitter", "parquet-hadoop-bundle")
        )
      }
    }:_*
  )

  def sparkRepl(
    v: String) = "org.apache.spark" %% "spark-repl" % v excludeAll ExclusionRule("org.apache.hadoop")

  def sparkSQL(v: String) = "org.apache.spark" %% "spark-sql" % v excludeAll(
    ExclusionRule("org.apache.hadoop")
  ) excludeAll(parquetList:_*)

  def sparkCSV: Seq[ModuleID] = {
    import scala.math.Ordering.Implicits._
    if (sparkVersionTuple >= (1, 3, 0)) {
      Seq("com.databricks" %% "spark-csv" % "1.3.0")
    } else Nil
  }

  def hadoopClient(v: String) = "org.apache.hadoop" % "hadoop-client" % v excludeAll(
    ExclusionRule("org.apache.commons", "commons-exec"),
    ExclusionRule("commons-codec", "commons-codec"),
    ExclusionRule("javax.servlet", "servlet-api"),
    ExclusionRule("com.google.guava", "guava")
  )

  def yarnProxy(v: String) = "org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % v excludeAll(
      ExclusionRule("org.apache.commons", "commons-exec"),
      ExclusionRule("commons-codec", "commons-codec"),
      ExclusionRule("javax.servlet", "servlet-api"),
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
      case _ => defaultJets3tVersion
    }
    "net.java.dev.jets3t" % "jets3t" % v force() excludeAll ExclusionRule()
  }

  val commonsIO = "org.apache.commons" % "commons-io" % "1.3.2"
  val commonsHttp = "org.apache.httpcomponents" % "httpclient" % "4.3.4" excludeAll ExclusionRule("com.google.guava")
  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3" force()
  val commonsCodec = "commons-codec" % "commons-codec" % "1.10" force()

  val defaultGuavaVersion = sys.props.getOrElse("guava.version", "16.0.1") // 16.0.1 for cassandra connector 1.6-M1
  val guava = "com.google.guava" % "guava" % defaultGuavaVersion force()
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
  val wispDepSumac = "com.quantifind" %% "sumac" % "0.3.0"
  val geometryDeps = Seq(
    "org.wololo" % "jts2geojson" % "0.7.0" excludeAll (
        ExclusionRule("com.fasterxml.jackson.module", "jackson-module-scala"),
        ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
        ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
        ExclusionRule("com.fasterxml.jackson.module", "jackson-module-jsonSchema"),
        ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-joda")
      )
  )
  //"com.quantifind" %% "wisp" % "0.0.4" excludeAll(
  //  ExclusionRule("com.google.guava"),
  //  ExclusionRule("org.json4s"),
  //  ExclusionRule("net.databinder", "unfiltered-filter"),
  //  ExclusionRule("net.databinder", "unfiltered-jetty"),
  //  ExclusionRule("org.apache.commons", "commons-math3"),
  //  ExclusionRule("commons-io", "commons-io")
  //)
  // wisp deps on jackson-module-scala_2.10 v2.4 → guava v15
  // but spark → guava 14.0.1
  val customJacksonScala = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.4.4" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.4" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.4" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % "2.4.4" force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.4" force() excludeAll ExclusionRule("com.google.guava")
  )
}
