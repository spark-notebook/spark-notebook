import sbt._

object Dependencies {
  val mesosVersion = sys.props.getOrElse("mesos.version", "0.26.0") //todo 1.0.0

  val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

  def excludeSpecs2(module: ModuleID): ModuleID =
    module.excludeAll(ExclusionRule(organization = "org.specs2"))

  // must hard-force the jackson version in play-json to the one in spark
  // play v2.5 uses jackson "2.7.8" which conflicts with "2.6.5" used in spark
  // otherwise it would give java.lang.NoClassDefFoundError: Could not initialize class org.apache.spark.rdd.RDDOperationScope$
  // FIXME: even better long-term solution would be to separate repl-server and play-ui
  val jacksonVer = "[2.6.5, 2.6.5]" // as in spark v2.1
  val customJacksonScala = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVer force() excludeAll ExclusionRule("com.google.guava"),
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVer force() excludeAll ExclusionRule("com.google.guava")
  )

  val jacksonExclusions =  Seq(
    ExclusionRule("com.fasterxml.jackson.module", "jackson-module-scala"),
    ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
    ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310"),
    ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jdk8"),
    ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
    ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
    ExclusionRule("com.fasterxml.jackson.module", "jackson-module-jsonSchema"),
    ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-joda")
  )

  val akkaGuavaExclusions = Seq(
    ExclusionRule("com.typesafe.akka"),
    ExclusionRule("com.google.guava"))

  val playExclusions = akkaGuavaExclusions ++ jacksonExclusions

  val playDeps = Seq(
    "com.typesafe.play" %% "play" % "2.5.15" withSources() excludeAll(playExclusions: _*),
    "com.typesafe.play" %% "play-test" % "2.5.15" % "test" withSources() excludeAll(playExclusions: _*)
  ) ++ customJacksonScala

  val playJson = Seq(
    "com.typesafe.play" %% "play-json" % "2.5.15" withSources() excludeAll(playExclusions: _*)
  ) ++ customJacksonScala

  val pac4jVersion = "2.1.0"
  // see https://github.com/pac4j/play-pac4j-scala-demo/blob/20ccf821bc557347ca2e555fb1c85d4afea92366/build.sbt#L9-L29
  val pac4jSecurity = Seq(
    "org.pac4j" % "play-pac4j" % "3.0.1" changing() excludeAll(ExclusionRule("org.pac4j", "pac4j-core")),
    "org.pac4j" % "pac4j"  % pac4jVersion changing(),
    "org.pac4j" % "pac4j-kerberos" % pac4jVersion changing(),
    "org.pac4j" % "pac4j-core" % pac4jVersion changing(),
    "org.pac4j" % "pac4j-http" % pac4jVersion changing()
  )

  val rxScala = "io.reactivex" %% "rxscala" % "0.22.0"

  val defaultHadoopVersion = sys.props.getOrElse("hadoop.version", "2.7.3")

  val akkaGroup = if (defaultHadoopVersion.startsWith("1")) "org.spark-project.akka" else "com.typesafe.akka"
  val akkaVersion = if (defaultHadoopVersion.startsWith("1")) "2.3.4-spark" else "2.4.18"
  val akka = akkaGroup %% "akka-actor" % akkaVersion
  val akkaRemote = akkaGroup %% "akka-remote" % akkaVersion
  val akkaSlf4j = akkaGroup %% "akka-slf4j" % akkaVersion

  val scala_2_1X = "2\\.1([0-9])\\.[0-9]+.*".r
  val spark_X_Y = "[a-zA-Z]*([0-9]+)\\.([0-9]+)\\.([0-9]+).*".r
  val extractVs = "[a-zA-Z]*(\\d+)\\.(\\d+)\\.(\\d+).*".r

  val defaultSparkVersion = sys.props.getOrElse("spark.version", "2.1.1")

  val sparkVersionTuple = defaultSparkVersion match { case extractVs(v, m, p) =>  (v.toInt, m.toInt, p.toInt)}
  val defaultScalaVersion = sys.props.getOrElse("scala.version", "2.11.8") match {
    case x@scala_2_1X("0") =>
      throw new IllegalArgumentException(
        "Scala 2.10 is not supported anymore.\n" +
        "Use scala 2.11 or an older version of spark-notebook (<= 0.8.x)"
      )
      //      defaultSparkVersion match {
      //        case spark_X_Y("2", _, _)                => "2.10.6"
      //        case spark_X_Y(_, _, _)                  => x
      //      }
    case x@scala_2_1X("1") => defaultSparkVersion match {
      case spark_X_Y("2", _, _) => "2.11.8"
      case spark_X_Y(_, _, _) => x
    }
  }
  val defaultWithKubernetes = sys.props.getOrElse("with.kubernetes", "false").toBoolean
  def sparkKubernetes(v: String) = {
    if (!defaultWithKubernetes) {
      Seq()
    } else {
      defaultSparkVersion match {
        case spark_X_Y("2", minor, _) if minor.toInt >= 4 => Seq("org.apache.spark" %% "spark-kubernetes" % v)
        case spark_X_Y(_, _, _) => throw new IllegalArgumentException(
          "Kubernetes client mode is only supported in Spark >= 2.4.0.\n" +
          "Use -Dspark.version=2.4.0 -Dwith.kubernetes=true"
        )
      }

    }
  }

  def sparkCore(v: String) = "org.apache.spark" %% "spark-core" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.apache.ivy", "ivy")
  )
  def sparkYarn(v: String) = "org.apache.spark" %% "spark-yarn" % v excludeAll(
      ExclusionRule("org.apache.hadoop"),
      ExclusionRule("javax.servlet", "servlet-api"),
      ExclusionRule("javax.servlet", "javax.servlet-api"),
      ExclusionRule("org.mortbay.jetty", "servlet-api"),
      ExclusionRule("org.apache.ivy", "ivy")
    )

  val defaultWithHive = sys.props.getOrElse("with.hive", "false").toBoolean

  def sparkHive(v: String) = "org.apache.spark" %% "spark-hive" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("org.apache.ivy", "ivy"),
    ExclusionRule("javax.servlet", "servlet-api"),
    ExclusionRule("javax.servlet", "javax.servlet-api"),
    ExclusionRule("org.mortbay.jetty", "servlet-api")
  )
  def sparkRepl(v: String) = "org.apache.spark" %% "spark-repl" % v excludeAll (
      ExclusionRule("org.apache.hadoop"),
      ExclusionRule("javax.servlet", "servlet-api"),
      ExclusionRule("javax.servlet", "javax.servlet-api")
    )

  def sparkSQL(v: String) = "org.apache.spark" %% "spark-sql" % v excludeAll(
    ExclusionRule("org.apache.hadoop"),
    ExclusionRule("javax.servlet", "servlet-api"),
    ExclusionRule("javax.servlet", "javax.servlet-api")
  )

  val jerseyClient = "com.sun.jersey" % "jersey-client" % "1.19"

  val defaultWithMesos = sys.props.getOrElse("with.mesos", "true").toBoolean

  def sparkMesos(v: String) = {
    if (!defaultWithMesos) {
      Seq()
    } else {
      v match {
        case spark_X_Y("1", _, _) => Seq()
        case spark_X_Y("2", "0", _) => Seq()
        case _ => // since spark 2.1.x, we should add spark-mesos:
          Seq(
            "org.apache.spark" %% "spark-mesos" % v
          )
      }
    }
  }

  def hadoopClient(v: String) = "org.apache.hadoop" % "hadoop-client" % v excludeAll(
    ExclusionRule("org.apache.commons", "commons-exec"),
    ExclusionRule("commons-codec", "commons-codec"),
    ExclusionRule("javax.servlet", "servlet-api"),
    ExclusionRule("javax.servlet", "javax.servlet-api"),
    ExclusionRule("com.google.guava", "guava")
  )

  def yarnProxy(v: String) = "org.apache.hadoop" % "hadoop-yarn-server-web-proxy" % v excludeAll(
      ExclusionRule("org.apache.commons", "commons-exec"),
      ExclusionRule("commons-codec", "commons-codec"),
      ExclusionRule("javax.servlet", "servlet-api"),
      ExclusionRule("javax.servlet", "javax.servlet-api"),
      ExclusionRule("com.google.guava", "guava")
  )

  val defaultJets3tVersion = sys.props.getOrElse("jets3t.version", "0.7.1")

  def jets3t(jv: Option[String], hv: Option[String]) = {
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

  val commonsIO = "commons-io" % "commons-io" % "2.4"
  val commonsHttp = "org.apache.httpcomponents" % "httpclient" % "4.3.4" excludeAll ExclusionRule("com.google.guava")
  val commonsExec = "org.apache.commons" % "commons-exec" % "1.3" force()
  val commonsCodec = "commons-codec" % "commons-codec" % "1.10" force()

  // P.S. Play 2.4 uses guava 18.0; play 2.5 uses 19.0. seems OK for force it.
  val defaultGuavaVersion = sys.props.getOrElse("guava.version", "16.0.1") // 16.0.1 for cassandra connector 1.6-M1
  val guava = "com.google.guava" % "guava" % defaultGuavaVersion force()
  val slf4jLog4j = "org.slf4j" % "slf4j-log4j12" % "1.7.7"
  val log4j = "log4j" % "log4j" % "1.2.17"

  // for aether only
  // must exclude as netty moved from org.jboss.netty to io.netty
  val ningAsyncHttpClient = "com.ning" % "async-http-client" % "[1.6.5, 1.6.5]" force() exclude("org.jboss.netty", "netty") //"1.8.10"//"[1.6.5, 1.6.5]" force()

  // Viz
  val geometryDeps = Seq(
    // it uses jackson 2.8.1
    "org.wololo" % "jts2geojson" % "0.8.0" excludeAll(jacksonExclusions: _*)
  ) ++ customJacksonScala

}
