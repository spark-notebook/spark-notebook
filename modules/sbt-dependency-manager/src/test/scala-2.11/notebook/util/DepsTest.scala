package com.datafellas.utils

import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.scalatest._

class SimpleSpec extends FlatSpec with Matchers with BeforeAndAfterAll with Inside {
  val sparkVersion = "10.0.0"
  val groupId = "com.example"
  val artifactId = "art"
  val version = "1.0.0"
  val classifier = "models-french"

  def checkDependencyMatch(d: ArtifactMD, t: ArtifactMD) = {
    assert(d.group == t.group)
    assert(d.artifact == t.artifact)
    assert(d.version == t.version)
    assert(d.extension == t.extension)
    assert(d.classifier == t.classifier)
  }

  "a dependency string without scalaversion, scope, classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version", sparkVersion)
    assert(d.get == ArtifactMD.from(new DefaultArtifact(groupId, artifactId, null, "jar", version)))
  }
  "a dependency string in scope test without scalaversion, classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version % test", sparkVersion)
    assert(d.get == ArtifactMD.from(new DefaultArtifact(groupId, artifactId, null, "test", version)))
  }
  "a dependency string in scope test with scala version without classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId %% $artifactId % $version % test", sparkVersion).get
    val t = ArtifactMD.from(new DefaultArtifact(groupId, artifactId+"_2.11", null, "test", version))
    checkDependencyMatch(d, t)
  }
  "a dependency string in scope test with scala version with classifier" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId %% $artifactId % $version % test classifier $classifier", sparkVersion).get
    val t = ArtifactMD.from(new DefaultArtifact(groupId, artifactId+"_2.11", classifier, "test", version))
    checkDependencyMatch(d, t)
  }
  "a dependency string in scope test with classifier without scala version" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version % test classifier $classifier", sparkVersion).get
    val t = ArtifactMD.from(new DefaultArtifact(groupId, artifactId, classifier, "test", version))
    checkDependencyMatch(d, t)
  }
  "a dependency string with classifier without scala version and scope" should "be parse correctly" in {
    val d = Deps.parseInclude(s"$groupId % $artifactId % $version classifier $classifier", sparkVersion).get
    val t = ArtifactMD.from(new DefaultArtifact(groupId, artifactId, classifier, "jar", version))
    checkDependencyMatch(d, t)
  }
}