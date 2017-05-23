package com.datafellas.g3nerator

import java.io.File

import com.datafellas.g3nerator.model.DependencyConfig
import notebook.Notebook

import scala.util.Try

// FIXME: to be implemented based on scala 2.10
class SbtDependencyResolver(snb: Notebook, sparkVersion: String, projectDependencyConfig: DependencyConfig) {
  def libraryDependenciesCode(customDeps: List[String]): String = {
    ""
  }

  def resolveJars(customDeps: List[String], repo: File): Try[List[String]] = {
    Try(List())
  }

  def resolversCode: String = {
    "Seq()"
  }
}
