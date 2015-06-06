package notebook.util

import sbt._

import scala.util.Try

object Deps extends java.io.Serializable {
  private val PATTERN_MODULEID_1 = """^\s*([^% ]+)\s*%\s*([^% ]+)\s*%\s*([^% ]+)\s*$""".r
  private val PATTERN_MODULEID_2 = """^\s*([^% ]+)\s*%\s*([^% ]+)\s*%\s*([^% ]+)\s*%\s*([^% ]+)\s*$""".r
  private val PATTERN_COORDINATE_1 = """^\s*([^:/ ]+)[:/]([^: ]+):([^: ]+)\s*$""".r

  def parseInclude(s: String): Option[ModuleID] = {
    s.headOption.filter(_ != '-').map(_ => s.dropWhile(_ == '+').trim).flatMap { line =>
      line.replaceAll("\"", "") match {
        case PATTERN_MODULEID_1(g, a, v) =>
          Some(g % a % v % "compile")
        case PATTERN_MODULEID_2(g, a, v, p) =>
          Some(g % a % v % p)
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(g % a % v % "compile")
        case _ =>
          None
      }
    }
  }

  def parsePartialExclude = (s: String) => s.trim match {
    case "_" => "*"
    case "" => "*"
    case x => x
  }

  def parseExclude(s: String): Option[ExclusionRule] = {
    s.headOption.filter(_ == '-').map(_ => s.dropWhile(_ == '-').trim).flatMap { line =>
      line.replaceAll("\"", "") match {
        case PATTERN_MODULEID_1(g, a, v) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = parsePartialExclude(a)))
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = parsePartialExclude(a)))
        case _ =>
          None
      }
    }
  }

  def resolve(includes: Seq[ModuleID], exclusions: Seq[ExclusionRule] = Nil)
      (implicit _resolvers: Seq[Resolver], repo: java.io.File) = {
    val logger: ConsoleLogger = ConsoleLogger(scala.Console.out)
    val resolvers = Resolver.file("local-repo", repo / "local")(Resolver.ivyStylePatterns) +: _resolvers
    val configuration: InlineIvyConfiguration = new InlineIvyConfiguration(
      new IvyPaths(repo.getParentFile, Some(repo)),
      resolvers, Nil, Nil, false, None, Nil, None,
      UpdateOptions(), logger
    )
    val ivy = new IvySbt(configuration)

    val deps: Seq[ModuleID] = includes map { include =>
      val thisExclusions = exclusions.filter { exclusion =>
        exclusion.organization != include.organization || exclusion.name != include.name
      }
      include.excludeAll(thisExclusions: _*)
    }
    val conf = InlineConfiguration(
      "org.scala-lang" % "scala" % notebook.BuildInfo.scalaVersion % "compile",
      ModuleInfo("dl deps"),
      deps,
      Set.empty,
      scala.xml.NodeSeq.Empty,
      Seq(Compile, Test, Runtime),
      None,
      Some(new IvyScala(
        scalaFullVersion = notebook.BuildInfo.scalaVersion,
        scalaBinaryVersion = cross.CrossVersionUtil.binaryScalaVersion(notebook.BuildInfo.scalaVersion),
        configurations = Nil,
        checkExplicit = true,
        filterImplicit = false,
        overrideScalaVersion = false
      ))
    )
    val module: IvySbt#Module = new ivy.Module(conf)

    val config: UpdateConfiguration = new UpdateConfiguration(None, //Some(new RetrieveConfiguration(baseDir, Resolver.defaultRetrievePattern)),
      false,
      UpdateLogging.Full
    )

    val files = try {
      val report: UpdateReport = IvyActions.update(module, config, logger)
      println(report)
      report.allFiles
    } catch {
      case x: Throwable =>
        scala.Console.err.println(x)
        Nil
    }

    val newJars = files.map(_.getPath).toSet.toList
    newJars
  }


  def script(cp: String, resolvers: List[Resolver], repo: java.io.File): Try[List[String]] = {
    //println(" -------------- DP --------------- ")
    val lines = cp.trim().split("\n").toList.map(_.trim()).filter(_.length > 0).toSet.toSeq
    val includes = lines map Deps.parseInclude collect { case Some(x) => x }
    //println(includes)
    val excludes = lines map Deps.parseExclude collect { case Some(x) => x }
    //println(excludes)

    val tryDeps = Try {
      Deps.resolve(includes, excludes)(resolvers, repo)
    }
    tryDeps
  }

}

object CustomResolvers extends java.io.Serializable {
  // enable S3 handlers
  fm.sbt.S3ResolverPlugin


  // TODO :proxy: val config = ConfigFactory.load().getConfig("remote-repos")
  // TODO :proxy: val proxy = Try(config.getConfig("proxy")).toOption
  private val authRegex = """(?s)^\s*\(([^\)]+)\)\s*$""".r
  private val credRegex = """"([^"]+)"\s*,\s*"([^"]+)"""".r //"

  def fromString(r: String): (String, Resolver) = try {
    val id :: tpe :: url :: flavor :: rest = r.split("%").toList.map(_.trim)

    val (username, password): (Option[String], Option[String]) = rest.headOption.map {
      case authRegex(usernamePassword) =>
        val (username, password) = usernamePassword match {
          case credRegex(username, password) => (username, password)
        }
        val u = if (username.startsWith("$")) sys.env.get(username.tail).get else username
        val p = if (password.startsWith("$")) sys.env.get(password.tail).get else password
        (Some(u), Some(p))
      case _ => (None, None)
    }.getOrElse((None, None))

    val rem = flavor match {
      case r if url.startsWith("s3") => new fm.sbt.S3RawRepository(id).atS3(url)
      case "maven" => new MavenRepository(id, url)
      case "ivy" => Resolver.url(id, new URL(url))(Resolver.ivyStylePatterns)
    }

    for {
      user <- username
      pwd <- password
    } {
      rem match {
        case s: RawRepository if s.resolver.isInstanceOf[fm.sbt.S3URLResolver] =>
          import com.amazonaws.SDKGlobalConfiguration.{ACCESS_KEY_SYSTEM_PROPERTY, SECRET_KEY_SYSTEM_PROPERTY}
          val bucket = url.dropWhile(_ != '/').drop("//".length).takeWhile(_ != '/')
          // @ see https://github.com/frugalmechanic/fm-sbt-s3-resolver/blob/3b400e9f9f51fb065608502715139823063274ce/src/main/scala/fm/sbt/S3URLHandler.scala#L59
          System.setProperty(s"$bucket.$ACCESS_KEY_SYSTEM_PROPERTY", user)
          System.setProperty(s"$bucket.$SECRET_KEY_SYSTEM_PROPERTY", pwd)
        case r =>
          val u = new java.net.URL(url)
          sbt.Credentials.add(id, u.getHost, user, pwd)
      }
    }

    val logR = r.replaceAll("\"", "\\\\\"")
    (logR, rem)
  } catch {
    case e: Throwable =>
      e.printStackTrace()
      println(s"CustomResolvers#fromString → Cannot parse $r")
      throw e
  }
}