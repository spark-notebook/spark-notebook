package com.datafellas.g3nerator

import java.io.File

import com.datafellas.g3nerator.model.DependencyConfig
import com.datafellas.utils.{Deps, CustomResolvers}
import notebook.Notebook
import sbt.{MavenRepository, Resolver}

import scala.util.Try


class SbtDependencyResolver(snb: Notebook, sparkVersion: String, projectDependencyConfig: DependencyConfig) {
  def libraryDependenciesCode(customDeps: List[String]): String = {
    val (includes, excludes) = Deps.parse(customDeps.mkString("\n"), sparkVersion)
    val eee = excludes.map { e =>
      if (e.name == "*") {
        s""" ExclusionRule( "${e.organization}" ) """
      } else {
        s""" ExclusionRule( "${e.organization}", "${e.name}" ) """
      }
    }.mkString(",\n")
    val deps = includes.map { i =>
      s"""|
          |libraryDependencies += "${i.organization}" % "${i.name}" % "${i.revision}" excludeAll(
          |$eee
          |)
          |
          |
    """.stripMargin
    }.mkString("\n")
    deps
  }

  def resolveJars(customDeps: List[String], repo: File): Try[List[String]] = {
    Deps.script(customDeps.mkString("\n"), resolvers, repo, sparkVersion)
  }

  lazy val resolvers: List[Resolver] = {
    val mavenLocal = Resolver.mavenLocal
    val mavenReleases = sbt.DefaultMavenRepository
    val typesafeReleases = Resolver.typesafeIvyRepo("releases")
    val jCenterReleases = Resolver.jcenterRepo
    val sonatypeReleases = Resolver.sonatypeRepo("releases")
    val spReleases = new MavenRepository("spark-packages", projectDependencyConfig.resolverBintraySparkPackagesMvn)
    val defaults = mavenLocal :: mavenReleases :: spReleases :: typesafeReleases :: jCenterReleases :: sonatypeReleases :: Nil
    snb.metadata.get.customRepos.getOrElse(List.empty[String]).map(CustomResolvers.fromString).map(_._2) ::: defaults
  }

  def resolversCode: String = {
    val resv = resolvers.map { r =>
      val root = r.getClass.getMethods.find(_.getName == "root")
      root match {
        case Some(m) => s""" "${r.name}" at "${m.invoke(r)}" """
        case None => r match {
          case r:sbt.URLRepository =>
            val patterns = s"""new sbt.Patterns(
              ${r.patterns.ivyPatterns.map(s => "\""+s+"\"").mkString("List(", ",", ")")},
              ${r.patterns.artifactPatterns.map(s => "\""+s+"\"").mkString("List(", ",", ")")},
              ${r.patterns.isMavenCompatible}
            )
            """
            s""" new sbt.URLRepository("${r.name}", $patterns) """
          case _ => "UNKNOWN repo →→→ " + r
        }
      }
    }
    resv.mkString("Seq(\n", ",\n", ")")
  }
}
