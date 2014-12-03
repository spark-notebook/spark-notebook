package notebook.util

import scala.collection.JavaConversions._
import com.jcabi.aether.Aether
import java.util.Arrays
import org.apache.maven.project.MavenProject
import org.sonatype.aether.repository.RemoteRepository
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.util.artifact.DefaultArtifact
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

case class ArtifactMD(group:String, artifact:String, version:String)
case class ArtifactSelector(group:Option[String]=None, artifact:Option[String]=None, version:Option[String]=None)
object ArtifactSelector {
  def apply(group:String, artifact:String, version:String):ArtifactSelector =
    ArtifactSelector(Some(group), Some(artifact), Some(version))
  def group(group:String) =
    ArtifactSelector(group=Some(group))
  def artifact(group:String, artifact:String) =
    ArtifactSelector(group=Some(group), artifact=Some(artifact))
}

object Deps extends java.io.Serializable {
  type ArtifactPredicate = PartialFunction[(ArtifactMD, Set[ArtifactMD]), Boolean]

  def parseInclude(s:String):Option[ArtifactMD] = {
    s.headOption.filter(_ != '-').map(_ => s.dropWhile(_=='+').trim).flatMap { line =>
      line.split("%").toList match {
        case List(g, a, v) =>
          Some(ArtifactMD(g.trim, a.trim, v.trim))
        case _             =>
          None
      }
    }
  }

  def parsePartialExclude = (s:String) => s.trim match {
    case "_" => None
    case ""  => None
    case x   => Some(x)
  }
  def parseExclude(s:String):Option[ArtifactSelector] = {
    s.headOption.filter(_ == '-').map(_ => s.dropWhile(_=='-').trim).flatMap { line =>
      line.split("%").toList match {
        case List(g, a, v) =>
          Some(ArtifactSelector(parsePartialExclude(g), parsePartialExclude(a), parsePartialExclude(v)))
        case _             =>
          None
      }
    }
  }

  def matchAMD(selector:ArtifactSelector, a:ArtifactMD) =
      selector.group.getOrElse(a.group) == a.group &&
      selector.artifact.getOrElse(a.artifact) == a.artifact &&
      selector.version.getOrElse(a.version) == a.version

  def transitiveExclude(selector:ArtifactSelector):ArtifactPredicate = {
    case (a, _) if matchAMD(selector, a) => true
    case (_, xs) => {
      val p:PartialFunction[ArtifactMD,Boolean] = { case a if matchAMD(selector, a) => true }
      xs exists (x => p.isDefinedAt(x) && p(x))
    }
  }


  def resolve (include:ArtifactMD, exclusions:Set[ArtifactPredicate]=Set.empty)
              (implicit remotes:List[RemoteRepository], repo:java.io.File) = {
    val exc = new DependencyFilter {
      def accept(node:DependencyNode, parents:java.util.List[DependencyNode] ):Boolean = {
        val ex = exclusions exists { case f =>
                  val na = node.getDependency.getArtifact
                  val a = ArtifactMD(na.getGroupId, na.getArtifactId, na.getVersion)
                  val sa = parents.map(n => n.getDependency.getArtifact)
                                  .map(na => ArtifactMD(na.getGroupId, na.getArtifactId, na.getVersion))
                                  .toSet
                  f.isDefinedAt((a, sa)) && f(a, sa)
                }
        !ex
      }
    }

    val deps:Set[Artifact] =  new Aether(remotes, repo).resolve(
                                new DefaultArtifact(include.group, include.artifact, "", "jar", include.version),
                                "runtime",
                                exc
                              ).toSet

    val newJars = deps.map(_.getFile.getPath).toSet.toList
    newJars

  }

}