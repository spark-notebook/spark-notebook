package com.datafellas.utils

import sbt._

import org.scalatest._

class SimpleSpec extends FlatSpec with Matchers with BeforeAndAfterAll with Inside {
  val sparkVersion = "10.0.0"
  val groupId = "com.example"
  val artifactId = "art"
  val version = "1.0.0"
  val classifier = "models-french"

  def checkDependencyMatch(d: ModuleID, t: ModuleID) = {
    assert(d.organization == t.organization)
    assert(d.name == t.name)
    assert(d.revision == t.revision)
    assert(d.configurations == t.configurations)
    assert(d.crossVersion.toString == t.crossVersion.toString)
    assert(d.explicitArtifacts == t.explicitArtifacts)
  }

  "a dependency string without scalaversion, scope, classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version", sparkVersion)
    assert(d.get == groupId % artifactId % version % "compile")
  }
  "a dependency string in scope test without scalaversion, classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version % test", sparkVersion)
    assert(d.get == groupId % artifactId % version % "test")
  }
  "a dependency string in scope test with scala version without classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId %% $artifactId % $version % test", sparkVersion).get
    val t = groupId %% artifactId % version % "test"
    checkDependencyMatch(d, t)
  }
  "a dependency string in scope test with scala version with classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId %% $artifactId % $version % test classifier $classifier", sparkVersion).get
    val t = groupId %% artifactId % version % "test" classifier (classifier)
    checkDependencyMatch(d, t)
  }
  "a dependency string in scope test with classifier without scala version" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version % test classifier $classifier", sparkVersion).get
    val t = groupId % artifactId % version % "test" classifier (classifier)
    checkDependencyMatch(d, t)
  }
  "a dependency string with classifier without scala version and scope" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version classifier $classifier", sparkVersion).get
    val t = groupId % artifactId % version % "compile" classifier (classifier)
    checkDependencyMatch(d, t)
  }
}