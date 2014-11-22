package notebook.util

import org.sonatype.aether.repository.RemoteRepository
import com.jcabi.aether.Aether
import java.util.Arrays
import org.apache.maven.project.MavenProject
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.util.artifact.DefaultArtifact
import scala.collection.JavaConversions._
import org.sonatype.aether.graph._
import org.sonatype.aether.util.filter.ExclusionsDependencyFilter

object Repos extends java.io.Serializable {
  @transient val central = new RemoteRepository(
    "maven-central",
    "default",
    "http://repo1.maven.org/maven2/"
  )

  def apply(id:String, name:String, url:String) = new RemoteRepository(id, name, url)
}

object Deps extends java.io.Serializable {
  type ArtifactMD = (String, String, String)
  type ArtifactPredicate = PartialFunction[(ArtifactMD, Set[ArtifactMD]), Boolean]

  def matchAMD(group:Option[String], artifact:Option[String], version:Option[String], a:ArtifactMD) =
      group.getOrElse(a._1) == a._1 && artifact.getOrElse(a._2) == a._2 && version.getOrElse(a._3) == a._3

  def transitiveExclude(group:Option[String]=None, artifact:Option[String]=None, version:Option[String]=None):ArtifactPredicate = {
    case (a, _) if matchAMD(group, artifact, version, a) => true
    case (_, xs) => {
        val p:PartialFunction[ArtifactMD,Boolean] = { case a if matchAMD(group, artifact, version, a) => true }
        xs exists (x => p.isDefinedAt(x) && p(x))
      }
  }

  def resolveAndAddToJars ( group:String, artifact:String, version:String,
                            exclusions:Set[ArtifactPredicate]=Set.empty
                          )(implicit remotes:List[RemoteRepository], repo:java.io.File) = {
    val exc = new DependencyFilter {
      def accept(node:DependencyNode, parents:java.util.List[DependencyNode] ):Boolean = {
        val ex = exclusions exists { case f =>
                  val na = node.getDependency.getArtifact
                  val a = (na.getGroupId, na.getArtifactId, na.getVersion)
                  val sa = parents.map(n => n.getDependency.getArtifact)
                                  .map(na => (na.getGroupId, na.getArtifactId, na.getVersion))
                                  .toSet
                  f.isDefinedAt((a, sa)) && f(a, sa)
                }
        !ex
      }
    }

    val deps:Set[Artifact] =  new Aether(remotes, repo).resolve(
                                new DefaultArtifact(group, artifact, "", "jar", version),
                                "runtime",
                                exc
                              ).toSet

    val newJars = deps.map(_.getFile.getPath).toSet.toList
    newJars
  }
}