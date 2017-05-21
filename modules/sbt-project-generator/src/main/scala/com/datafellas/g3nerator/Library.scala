package com.datafellas.g3nerator

import com.datafellas.g3nerator.model.{DebianName, LibraryName, PackageName}


case class Library(notebookName:String, groupPackage:String, version: String, majorScalaVersion:String) {
  val name = LibraryName(notebookName).name
  val group = groupPackage // project.config.pkg
  val artifact = name + "_" + majorScalaVersion
  val classPkg = groupPackage+ "." + PackageName(notebookName).name
  val jarInDeb = s"$group.$name-$version.jar"
  val debianPackage = s"${DebianName(notebookName).name}_${version}_all.deb"

  def jarInTargetPath = s"target/scala-$majorScalaVersion/$group.${name}_$majorScalaVersion-$version.jar"
}
