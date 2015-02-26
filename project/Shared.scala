import sbt._
import Keys._

import Dependencies._

object Shared {
  lazy val sparkVersion = SettingKey[String]("x-spark-version")

  lazy val hadoopVersion = SettingKey[String]("x-hadoop-version")

  lazy val jets3tVersion = SettingKey[String]("x-jets3t-version")

  lazy val sharedSettings:Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.10.4",
    sparkVersion  :=  defaultSparkVersion,
    hadoopVersion :=  defaultHadoopVersion,
    jets3tVersion :=  defaultJets3tVersion,
    libraryDependencies += guava
  )

  val repl:Seq[Def.Setting[_]] =  {
    val lib = libraryDependencies <++=  (sparkVersion, hadoopVersion, jets3tVersion) { (sv, hv, jv) =>
                                          if (sv != "1.2.0") Seq(sparkRepl(sv)) else Seq.empty
                                        }
    val unmanaged = 
      unmanagedJars in Compile  ++= (if (sparkVersion.value == "1.2.0") 
                                      Seq((baseDirectory in "sparkNotebook").value / "temp/spark-repl_2.10-1.2.0-notebook.jar")
                                    else 
                                      Seq.empty)
                                    
    lib ++ unmanaged
  }

  lazy val sparkSettings:Seq[Def.Setting[_]] = Seq(
    libraryDependencies <++= (sparkVersion, hadoopVersion, jets3tVersion) { (sv, hv, jv) =>
      val libs = Seq(
        //sparkRepl(sv), → spark-repl:1.2.0 not yet published → lib/spark-repl_2.10-1.2.0-notebook.jar to be used
        breeze,
        sparkSQL(sv),
        hadoopClient(hv),
        jets3t(jv),
        commonsCodec
      )
      libs
    }
  ) ++  repl
}
