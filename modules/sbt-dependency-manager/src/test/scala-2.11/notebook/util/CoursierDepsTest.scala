package notebook.util

import com.datafellas.utils.Repos
import coursier.{Attributes, Cache, Dependency, Module}
import coursier.maven.MavenRepository
import org.scalatest._
import org.sonatype.aether.repository.RemoteRepository

class CoursierDepsTest extends WordSpec with Matchers with BeforeAndAfterAll with Inside {
  val sparkVersion = "10.0.0"
  val version = "0.1.1"
  val classifier = "some-classifier"
  val scalaVer = "2.11"

  // based on ReplCalculator
  val remotes: List[RemoteRepository] = List(Repos.mavenLocal, Repos.central, Repos.sparkPackages, Repos.oss)

  val (repos, deps) = CoursierDeps.parseCoursierDependencies(
    cp =
      s"""|com.someorg % java-artifact % $version
          |com.someorg %% scala-artifact % $version
          |com.someorg %% scala-artifact-cl % $version classifier $classifier
          |- org.excludeme : excluded-artifact-mavenlike : _
          |- org.excluded.org : _ : _
          |- org.excludeme % excluded-artifact-java-sbtlike % _
          |- org.excludeme %% excluded-artifact-scala-sbtlike % _
          |- org.excludeme.sbtlike % _ % _
          |""".stripMargin,
    remotes = remotes,
    sparkVersion = "2.1.1"
  )

  "yield repos" in {
    repos shouldBe Seq(
      MavenRepository(Repos.mavenLocal.getUrl),
      MavenRepository("http://repo1.maven.org/maven2/"),
      MavenRepository("http://dl.bintray.com/spark-packages/maven/"),
      MavenRepository("https://oss.sonatype.org/content/repositories/releases/"),
      Cache.ivy2Local
    )
  }

  "yield dependencies with excludes (ignoring version in exclude)" in {
    val expectedExcludes = Set(
      ("org.excludeme", "excluded-artifact-java-sbtlike"),
      ("org.excludeme", "excluded-artifact-scala-sbtlike_2.11"),
      ("org.excludeme", "excluded-artifact-mavenlike"),
      // '*' denotes an exclude by org, see https://goo.gl/mKGKoz
      ("org.excludeme.sbtlike", "*"),
      ("org.excluded.org", "*")
    )
    deps shouldBe Set(
      Dependency(Module("com.someorg", "java-artifact"), version, exclusions = expectedExcludes),
      Dependency(Module("com.someorg", s"scala-artifact_$scalaVer"), version, exclusions = expectedExcludes),
      Dependency(Module("com.someorg", s"scala-artifact-cl_$scalaVer"), version, exclusions = expectedExcludes, attributes = Attributes(classifier=classifier))
    )
  }
}