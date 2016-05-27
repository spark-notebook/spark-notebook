import Dependencies._
import sbt.Keys._
import sbt._

object Shared {
  lazy val sparkVersion = SettingKey[String]("x-spark-version")

  lazy val hadoopVersion = SettingKey[String]("x-hadoop-version")

  lazy val jets3tVersion = SettingKey[String]("x-jets3t-version")

  lazy val jlineDef = SettingKey[(String, String)]("x-jline-def")

  lazy val withHive = SettingKey[Boolean]("x-with-hive")

  lazy val withParquet = SettingKey[Boolean]("x-with-parquet")

  lazy val sharedSettings: Seq[Def.Setting[_]] = Seq(
    scalaVersion := defaultScalaVersion,
    sparkVersion := defaultSparkVersion,
    hadoopVersion := defaultHadoopVersion,
    jets3tVersion := defaultJets3tVersion,
    jlineDef := (if (defaultScalaVersion.startsWith("2.10")) {
      ("org.scala-lang", defaultScalaVersion)
    } else {
      ("jline", "2.12")
    }),
    withHive := defaultWithHive,
    withParquet := defaultWithParquet,
    libraryDependencies += guava
  )

  val wispSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies += wispDepSumac,
    unmanagedJars in Compile ++= (
      if (scalaVersion.value.startsWith("2.10"))
        Seq((baseDirectory in "sparkNotebook").value / "temp" / "wisp_2.10-0.0.5.jar")
      else
        Seq((baseDirectory in "sparkNotebook").value / "temp" / "wisp_2.11-0.0.5.jar")
    )
  )

  val gisSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= geometryDeps
  )

  val repl: Seq[Def.Setting[_]] = {
    val lib = libraryDependencies <++= (sparkVersion, hadoopVersion, jets3tVersion) {
      (sv, hv, jv) => if (sv != "1.2.0") Seq(sparkRepl(sv)) else Seq.empty
    }
    val unmanaged = unmanagedJars in Compile ++= (
      if (sparkVersion.value == "1.2.0" && !scalaVersion.value.startsWith("2.11"))
        Seq((baseDirectory in "sparkNotebook").value / "temp/spark-repl_2.10-1.2.0-notebook.jar")
      else
        Seq.empty
      )

    val repos = resolvers <++= sparkVersion { (sv) =>
      if (sv == "1.2.0") {
        Seq("Resolver for spark-yarn 1.2.0" at "https://github.com/adatao/mvnrepos/raw/master/releases") // spark-yarn 1.2.0 is not released
      } else {
        Nil
      }
    }

    lib ++ unmanaged ++ repos
  }

  val hive: Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (withHive, sparkVersion) { (wh, sv) =>
      if (wh) List(sparkHive(sv)) else Nil
    }
  )

  val yarnWebProxy: Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (hadoopVersion) { (hv) =>
      if (!hv.startsWith("1")) List(yarnProxy(hv)) else Nil
    }
  )

  lazy val sparkSettings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (scalaVersion, sparkVersion, hadoopVersion, jets3tVersion) { (v, sv, hv, jv) =>
      val jets3tVersion = sys.props.get("jets3t.version") match {
        case Some(jv) => jets3t(Some(jv), None)
        case _ => jets3t(None, Some(hv))
      }

      val jettyVersion = "8.1.14.v20131031"

      val libs = Seq(
        breeze,
        sparkCore(sv),
        sparkYarn(sv),
        sparkSQL(sv),
        hadoopClient(hv),
        jets3tVersion,
        commonsCodec
      ) ++ sparkCSV ++ (
            if (!v.startsWith("2.10")) {
              // in 2.11
              //Boot.scala → HttpServer → eclipse
              // eclipse → provided boohooo :'-(
              Seq(
                "org.eclipse.jetty" % "jetty-http"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
                "org.eclipse.jetty" % "jetty-servlet"      % jettyVersion,
                "org.eclipse.jetty" % "jetty-util"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-security"     % jettyVersion,
                "org.eclipse.jetty" % "jetty-plus"         % jettyVersion,
                "org.eclipse.jetty" % "jetty-server"       % jettyVersion
              )
            } else Nil
          )
      libs
    }
  ) ++ repl ++ hive ++ yarnWebProxy

  lazy val tachyonSettings: Seq[Def.Setting[_]] = {
    def tachyonVersion(sv: String) =
      sv.dropWhile(!_.isDigit) /*get rid of the v in v1.6.0 for instance */
        .takeWhile(_ != '-' /*get rid of -SNAPSHOT, -RC or whatever*/)
        .split("\\.")
        .toList
        .map(_.toInt).take(3) match {
          case List(1, y, z) if y <= 3 => "0.5.0"
          case List(1, 4, z) => "0.6.4"
          case List(1, 6, z) => "0.8.2"
          case List(1, y, z) => "0.7.1"
          case List(2, y, z) =>
            // need to change to use Alluxio instead...
            // http://search.maven.org/#search|ga|1|alluxio
            // http://www.alluxio.com/2016/04/getting-started-with-alluxio-and-spark/
            "0.7.1"
          case _ => throw new IllegalArgumentException("Bad spark version for tachyon: " + sv)
        }

    val exludes = Seq(
      ExclusionRule("org.jboss.netty", "netty"),
      ExclusionRule("org.apache.hadoop",  "hadoop-client"),
      ExclusionRule("org.apache.curator", "curator-recipes"),
      ExclusionRule("org.tachyonproject", "tachyon-underfs-glusterfs"),
      ExclusionRule("org.tachyonproject", "tachyon-underfs-s3"),
      ExclusionRule("com.fasterxml.jackson.module", "jackson-module-scala"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
      ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
      ExclusionRule("com.fasterxml.jackson.module", "jackson-module-jsonSchema"),
      ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-joda")
    )

    val deps = sparkVersion { sv =>
      tachyonVersion(sv) match {
        case x@("0.8.2"|"0.7.1")        =>
          Seq(
          "org.tachyonproject" % "tachyon-common" % x excludeAll(exludes:_*),
          "org.tachyonproject" % "tachyon-client" % x excludeAll(exludes:_*),
          "org.tachyonproject" % "tachyon-servers" % x excludeAll(exludes:_*),
          "org.tachyonproject" % "tachyon-minicluster" % x excludeAll(exludes:_*)
        )
        case x =>
          Seq(
          "org.tachyonproject" % "tachyon" % tachyonVersion(sv) excludeAll(exludes:_*),
          "org.tachyonproject" % "tachyon-client" % tachyonVersion(sv) excludeAll(exludes:_*),
          "org.tachyonproject" % "tachyon" % tachyonVersion(sv) classifier "tests" excludeAll(exludes:_*)
        )
      }
    }

    Seq(
      unmanagedSourceDirectories in Compile <+= (sparkVersion, sourceDirectory in Compile) {
        (sv, sd) => sd / ("tachyon_" + tachyonVersion(sv))
      },
      libraryDependencies <++= deps
    )
  }
}
