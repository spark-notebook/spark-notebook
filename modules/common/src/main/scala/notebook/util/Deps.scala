package notebook.util

import java.util.Arrays

import scala.collection.JavaConversions._
import scala.util.Try

import com.typesafe.config.{ConfigFactory, Config}

import sbt._

object Deps extends java.io.Serializable {
  def parseInclude(s:String):Option[ModuleID] = {
    s.headOption.filter(_ != '-').map(_ => s.dropWhile(_=='+').trim).flatMap { line =>
      line.replaceAll("\"", "").split("%").toList match {
        case List(g, a, v) =>
          Some(g.trim % a.trim % v.trim % "compile")
        case List(g, a, v, p) =>
          Some(g.trim % a.trim % v.trim % p.trim)
        case _             =>
          None
      }
    }
  }

  def parsePartialExclude = (s:String) => s.trim match {
    case "_" => "*"
    case ""  => "*"
    case x   => x
  }
  def parseExclude(s:String):Option[ExclusionRule] = {
    s.headOption.filter(_ == '-').map(_ => s.dropWhile(_=='-').trim).flatMap { line =>
      line.replaceAll("\"", "").split("%").toList match {
        case List(g, a, v) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = parsePartialExclude(a)))
        case _             =>
          None
      }
    }
  }

  def resolve (includes:Seq[ModuleID], exclusions:Seq[ExclusionRule]=Nil)
              (implicit _resolvers:Seq[Resolver], repo:java.io.File) = {
    val logger: ConsoleLogger = ConsoleLogger(scala.Console.out)
    val resolvers = Resolver.file("local-repo",  repo / "local")(Resolver.ivyStylePatterns) +: _resolvers
    val configuration: InlineIvyConfiguration = new InlineIvyConfiguration(
                                                      new IvyPaths( repo.getParentFile, Some(repo)),
                                                      resolvers, Nil, Nil, false, None, Nil, None,
                                                      UpdateOptions(), logger
                                                    )
    val ivy = new IvySbt(configuration)

    val deps:Seq[ModuleID] = includes map { include => include.excludeAll(exclusions:_*)}

    val conf = InlineConfiguration(
      "org.scala-lang" % "scala" % (notebook.BuildInfo.scalaVersion/*notebook.BuildInfo.scalaVersion*/) % "compile",
      ModuleInfo("dl deps"),
      deps,
      Set.empty,
      scala.xml.NodeSeq.Empty,
      Seq(Compile, Test, Runtime),
      None,
      Some(new IvyScala(
        scalaFullVersion = notebook.BuildInfo.scalaVersion/*notebook.BuildInfo.scalaVersion*/,
        scalaBinaryVersion = cross.CrossVersionUtil.binaryScalaVersion(notebook.BuildInfo.scalaVersion/*notebook.BuildInfo.scalaVersion*/),
        configurations = Nil,
        checkExplicit = true,
        filterImplicit = false,
        overrideScalaVersion = false
      ))
    )
    val module: IvySbt#Module = new ivy.Module(conf)

    val config: UpdateConfiguration = new UpdateConfiguration(  None,//Some(new RetrieveConfiguration(baseDir, Resolver.defaultRetrievePattern)),
                                                                false,
                                                                UpdateLogging.Full
                                                              )

    val updateEither: Either[UnresolvedWarning, UpdateReport] = IvyActions.updateEither(module, config, UnresolvedWarningConfiguration(), logger)
    val files = updateEither match {
      case Left(x) =>
        scala.Console.err.println(x)
        Nil
      case Right(report) =>
        println(report)
        report.allFiles
    }

    val newJars = files.map(_.getPath).toSet.toList
    newJars
  }


  def script(cp:String, resolvers:List[Resolver], repo:java.io.File):Try[List[String]] = {
    //println(" -------------- DP --------------- ")
    val lines = cp.trim().split("\n").toList.map(_.trim()).filter(_.size > 0).toSet.toSeq
    val includes = lines map (Deps.parseInclude _) collect { case Some(x) => x }
    //println(includes)
    val excludes = lines map (Deps.parseExclude _) collect { case Some(x) => x }
    //println(excludes)

    val tryDeps = Try {
      Deps.resolve(includes, excludes)(resolvers, repo)
    }
    tryDeps
  }

}


object CustomResolvers extends java.io.Serializable {
  // TODO :proxy: val config = ConfigFactory.load().getConfig("remote-repos")
  // TODO :proxy: val proxy = Try(config.getConfig("proxy")).toOption

  def fromString(r:String):(String, Resolver) = {
    val id::tpe::url::flavor::rest = r.split("%").toList

    //TODO : val (username, password):(Option[String],Option[String]) = rest.headOption.map { auth =>
    //TODO :   auth match {
    //TODO :     case authRegex(usernamePassword)   =>
    //TODO :       val (username, password) = usernamePassword match { case credRegex(username, password) => (username, password) }
    //TODO :       val u = if (username.startsWith("$")) sys.env.get(username.tail).get else username
    //TODO :       val p = if (password.startsWith("$")) sys.env.get(password.tail).get else password
    //TODO :       (Some(u), Some(p))
    //TODO :     case _                             => (None, None)
    //TODO :   }
    //TODO : }.getOrElse((None, None))

    val rem = flavor match {
      case "maven" => new MavenRepository(id.trim, url.trim)//Repos(id.trim,tpe.trim,url.trim,username,password)
      case "ivy"   => Resolver.url(id.trim, new URL(url.trim))(Resolver.ivyStylePatterns)
    }
    val logR = r.replaceAll("\"", "\\\\\"")
    (logR, rem)
  }


  def apply(id:String, name:String, url:String, username:Option[String] = None, password:Option[String] = None):Option[Resolver] = {
    //val r = new RemoteRepository(id, name, url)
    //for {
    //  u <- username
    //  p <- password
    //} {
    //  r.setAuthentication(new Authentication(u, p))
    //}
    //for {
    //  p        <- proxy
    //  protocol <- Try(p.getString("protocol")).toOption
    //  host     <- Try(p.getString("host")).toOption
    //  port     <- Try(p.getInt("port")).toOption
    //} {
    //  val auth = (for {
    //    username <- Try(p.getString("username")).toOption
    //    password <- Try(p.getString("password")).toOption
    //  } yield new Authentication(username, password)).getOrElse(null)
    //  val px = new AetherProxy(protocol, host, port, auth)
    //  r.setProxy(px)
    //}
    //r
    ???
  }

  //alias for clarity
  def s3(id:String, name:String, url:String, key:String, secret:String) = ???//Repos.apply(id, name, url, Some(key), Some(secret))
}