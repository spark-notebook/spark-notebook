import sbt._
import Keys._

import Dependencies._

object Shared {
  lazy val sparkVersion = SettingKey[String]("x-spark-version")

  lazy val hadoopVersion = SettingKey[String]("x-hadoop-version")

  lazy val jets3tVersion = SettingKey[String]("x-jets3t-version")

  lazy val jlineDef      = SettingKey[(String, String)]("x-jline-def")

  lazy val sharedSettings:Seq[Def.Setting[_]] = Seq(
    scalaVersion        :=  defaultScalaVersion,
    sparkVersion        :=  defaultSparkVersion,
    hadoopVersion       :=  defaultHadoopVersion,
    jets3tVersion       :=  defaultJets3tVersion,
    jlineDef            :=  (if (defaultScalaVersion.startsWith("2.10")) {
                              ("org.scala-lang", defaultScalaVersion)
                            } else {
                              ("jline", "2.12")
                            }),
    libraryDependencies += guava
  )

  val repl:Seq[Def.Setting[_]] =  {
    val lib = libraryDependencies <++=  (sparkVersion, hadoopVersion, jets3tVersion) { (sv, hv, jv) =>
                                          if (sv != "1.2.0") Seq(sparkRepl(sv)) else Seq.empty
                                        }
    val unmanaged =
      unmanagedJars in Compile  ++= (if (sparkVersion.value == "1.2.0" && !scalaVersion.value.startsWith("2.11"))
                                      Seq((baseDirectory in "sparkNotebook").value / "temp/spark-repl_2.10-1.2.0-notebook.jar")
                                    else
                                      Seq.empty)

    val repos = resolvers <++= (sparkVersion) { (sv) =>
      if (sv == "1.2.0") {
        Seq("Resolver for spark-yarn 1.2.0" at "https://github.com/adatao/mvnrepos/raw/master/releases") // spark-yarn 1.2.0 is not released
      } else {
        Nil
      }
    }

    lib ++ unmanaged ++ repos
  }

  lazy val sparkSettings:Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (sparkVersion, hadoopVersion, jets3tVersion) { (sv, hv, jv) =>
      val libs = Seq(
        breeze,
        sparkCore(sv),
        sparkYarn(sv),
        sparkSQL(sv),
        hadoopClient(hv),
        jets3t(jv),
        commonsCodec
      )
      libs
    }
  ) ++  repl

  lazy val tachyonSettings:Seq[Def.Setting[_]] = {
    def tachyonVersion(sv:String) = (sv.split("\\.").toList.map(_.toInt)) match {
      case List(1, y, z) if y <= 3              => "0.5.0"
      case List(1, 3, z) if z > 0 /*hopefully*/ => "0.6.3"
      case _                                    => throw new IllegalArgumentException("Bad spark version for tachyon: " + sv)
    }

    Seq(
      unmanagedSourceDirectories in Compile <+= (sparkVersion, sourceDirectory in Compile) { (sv, sd) =>
        sd / ("tachyon_" + tachyonVersion(sv))
      },

      libraryDependencies <++= sparkVersion { sv =>
        Seq(
          "org.tachyonproject" % "tachyon" % tachyonVersion(sv),
          "org.tachyonproject" % "tachyon-client" % tachyonVersion(sv),
          "org.tachyonproject" % "tachyon" % tachyonVersion(sv) classifier "tests"
        )
      }
    )
  }
}