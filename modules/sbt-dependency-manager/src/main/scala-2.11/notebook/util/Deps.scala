package com.datafellas.utils

import java.io.{ IOException, File }
import java.net.URL
import java.util.Arrays

import scala.collection.JavaConversions._
import scala.util.{Failure, Try}
import scala.xml.{ Text, NodeSeq, Elem, XML }

import com.typesafe.config.{ConfigFactory, Config}

import org.apache.maven.project.MavenProject
import org.xml.sax.SAXParseException
import org.sonatype.aether.repository.{RemoteRepository, Proxy => AetherProxy}
import org.sonatype.aether.repository.Authentication
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.graph._
import org.sonatype.aether.util.filter.ExclusionsDependencyFilter
import com.jcabi.aether.Aether


object Repos extends java.io.Serializable {

  @transient val central = new RemoteRepository(
    "maven-central",
    "default",
    "http://repo1.maven.org/maven2/"
  )

  @transient val oss = new RemoteRepository(
    "oss-sonatype",
    "default",
    "https://oss.sonatype.org/content/repositories/releases/"
  )

  @transient val sparkPackages = new RemoteRepository(
    "spark-packages",
    "default",
    "http://dl.bintray.com/spark-packages/maven/"
  )

  @transient val mavenLocal = new RemoteRepository(
    "Maven2 local",
    "default",
    mavenLocalDir.toURI.toString
  )

  val config = ConfigFactory.load().getConfig("remote-repos")
  val proxy = Try(config.getConfig("proxy")).toOption

  // helper
  def apply(id:String, name:String, url:String, username:Option[String] = None, password:Option[String] = None) = {
    val r = new RemoteRepository(id, name, url)
    for {
      u <- username
      p <- password
    } {
      r.setAuthentication(new Authentication(u, p))
    }
    for {
      p        <- proxy
      protocol <- Try(p.getString("protocol")).toOption
      host     <- Try(p.getString("host")).toOption
      port     <- Try(p.getInt("port")).toOption
    } {
      val auth = (for {
        username <- Try(p.getString("username")).toOption
        password <- Try(p.getString("password")).toOption
      } yield new Authentication(username, password)).getOrElse(null)
      val px = new AetherProxy(protocol, host, port, auth)
      r.setProxy(px)
    }
    r
  }

  //alias for clarity
  def s3(id:String, name:String, url:String, key:String, secret:String) = Repos.apply(id, name, url, Some(key), Some(secret))

  private[this] def mavenLocalDir: File = {
    val userHome = new File(System.getProperty("user.home"))
    def loadHomeFromSettings(f: () => File): Option[File] =
      try {
        val file = f()
        if (!file.exists) None
        else ((XML.loadFile(file) \ "localRepository").text match {
          case ""    => None
          case e @ _ => Some(new File(e))
        })
      } catch {
        // Occurs inside File constructor when property or environment variable does not exist
        case _: NullPointerException => None
        // Occurs when File does not exist
        case _: IOException          => None
        case e: SAXParseException    => System.err.println(s"WARNING: Problem parsing ${f().getAbsolutePath}, ${e.getMessage}"); None
      }
    loadHomeFromSettings(() => new File(userHome, ".m2/settings.xml")) orElse
      loadHomeFromSettings(() => new File(new File(System.getenv("M2_HOME")), "conf/settings.xml")) getOrElse
      new File(userHome, ".m2/repository")
  }
}

case class ArtifactMD(group:String, artifact:String, version:String, extension:Option[String]=Some("jar"), classifier:Option[String]=None)
object ArtifactMD {
  def from(a:Artifact) = ArtifactMD(a.getGroupId, a.getArtifactId, a.getVersion, Option(a.getExtension), Option(a.getClassifier).filter(_.nonEmpty))
}
case class ArtifactSelector(group:Option[String]=None, artifact:Option[String]=None, version:Option[String]=None)
object ArtifactSelector {
  def apply(group:String, artifact:String, version:String):ArtifactSelector = ArtifactSelector(Some(group), Some(artifact), Some(version))
  def group(group:String)                                                   = ArtifactSelector(group=Some(group))
  def artifact(group:String, artifact:String)                               = ArtifactSelector(group=Some(group), artifact=Some(artifact))
}

object Deps extends java.io.Serializable {
  val logger = org.slf4j.LoggerFactory.getLogger("Aether downloads")

  type ArtifactPredicate = PartialFunction[(ArtifactMD, Set[ArtifactMD]), Boolean]

  private val PATTERN_MODULEID_1 = """^([^%\s]+)\s*%(%?)\s*([^%\s]+)\s*%\s*([^%\s]+)(?:\s+classifier\s+([^\s]+)\s*)?$""".r
  private val PATTERN_MODULEID_2 = """^([^%\s]+)\s*%(%?)\s*([^%\s]+)\s*%\s*([^%\s]+)\s*%\s*([^%\s]+)(?:\s+classifier\s+([^\s]+)\s*)?$""".r
  private val PATTERN_COORDINATE_1 = """^([^:/]+)[:/]([^:]+):([^:]+)$""".r

  def includeSparkVersion(v:String, sv:String) = v match {
    case "_" => sv
    case x   => x
  }

  def parseInclude(s:String, sv:String):Option[ArtifactMD] = {
    s.headOption.filter(_ != '-').map(_ => s.dropWhile(_=='+').trim).flatMap { line =>
      line.replaceAll("\"", "").trim match {
        case PATTERN_MODULEID_1(g, "%", a, v, c) =>
          Some(ArtifactMD(g, a+"_2.11", includeSparkVersion(v, sv), classifier=Option(c)))
        case PATTERN_MODULEID_1(g, "", a, v, c) =>
          Some(ArtifactMD(g, a, includeSparkVersion(v, sv), classifier=Option(c)))
        case PATTERN_MODULEID_2(g, "%", a, v, p, c) =>
          Some(ArtifactMD(g, a+"_2.11", includeSparkVersion(v, sv), Some(p), Option(c)))
        case PATTERN_MODULEID_2(g, "", a, v, p, c) =>
          Some(ArtifactMD(g, a, includeSparkVersion(v, sv), Some(p), Option(c)))
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(ArtifactMD(g, a, includeSparkVersion(v, sv)))
        case _ =>
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
      line.replaceAll("\"", "") match {
        case PATTERN_MODULEID_1(g, "", a, v, _) =>
          Some(ArtifactSelector(parsePartialExclude(g), parsePartialExclude(a), parsePartialExclude(v)))
        case PATTERN_MODULEID_1(g, "%", a, v, _) =>
          Some(ArtifactSelector(parsePartialExclude(g), Some(a+"_2.11"), parsePartialExclude(v)))
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(ArtifactSelector(parsePartialExclude(g), parsePartialExclude(a), parsePartialExclude(v)))
        case _ =>
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
                  val a = ArtifactMD.from(na)
                  val sa = parents.map(n => n.getDependency.getArtifact)
                                  .map(na => ArtifactMD.from(na))
                                  .toSet
                  f.isDefinedAt((a, sa)) && f(a, sa)
                }
        !ex
      }
    }

    //                 DefaultArtifact(String groupId, String artifactId, String classifier,              String extension,                   String version)
    val artifact = new DefaultArtifact(include.group, include.artifact, include.classifier.getOrElse(""), include.extension.getOrElse("jar"), include.version)
    val deps:Set[Artifact] =  new Aether(remotes, repo).resolve(
                                artifact,
                                "runtime",
                                exc
                              ).toSet
    logger.info(s"""|
      |Resolved/Downloaded:
      |${deps.map(x => "* " + x.toString).mkString("\n")}
      |===================================================
      |""".stripMargin
    )
    val newJars = deps.map(_.getFile.getPath).toSet.toList
    newJars
  }

  def parse(cp:String, sv:String) = {
    val lines = cp.trim().split("\n").toList.map(_.trim()).filter(_.size > 0).toSet
    val includes = lines.map(v => Deps.parseInclude(v, sv)).collect { case Some(x) => x }
    val excludes = lines map (Deps.parseExclude _) collect { case Some(x) => x }
    (includes, excludes)
  }

  def script(cp:String, remotes:List[RemoteRepository], repo:java.io.File, sparkVersion:String):Try[List[String]] = {
    val (includes, excludes) = parse(cp, sparkVersion)
    val excludesFns = excludes map (Deps.transitiveExclude _)

    val tryDeps:Try[List[String]] = includes.foldLeft(Try(List.empty[String])) { case (t, a) =>
      t flatMap { l => Try(l ::: Deps.resolve(a, excludesFns)(remotes, repo)) }
    }
    tryDeps match {
      case Failure(t) => logger.error("Failed to resolve dependencies: \n" + cp, t)
      case _ =>
    }
    tryDeps
  }

}
