package com.datafellas.utils

import sbt._

import scala.util.Try

object Deps extends java.io.Serializable {
  private val PATTERN_MODULEID_1 = """^([^%\s]+)\s*%(%?)\s*([^%\s]+)\s*%\s*([^%\s]+)(?:\s+classifier\s+([^\s]+)\s*)?$""".r
  private val PATTERN_MODULEID_2 = """^([^%\s]+)\s*%(%?)\s*([^%\s]+)\s*%\s*([^%\s]+)\s*%\s*([^%\s]+)(?:\s+classifier\s+([^\s]+)\s*)?$""".r
  private val PATTERN_COORDINATE_1 = """^([^:/]+)[:/]([^:]+):([^:]+)$""".r

  def includeSparkVersion(v:String, sv:String) = v match {
    case "_" => sv
    case x   => x
  }

  def includeClassifier(m:ModuleID, c:Option[String]) = c.map(c => m classifier c).orElse(Some(m))

  def parseInclude(s: String, sv:String): Option[ModuleID] = {
    s.headOption.filter(_ != '-').map(_ => s.dropWhile(_ == '+').trim).flatMap { line =>
      line.replaceAll("\"", "").trim match {
        case PATTERN_MODULEID_1(g, "%", a, v, c) =>
          val m = g %% a % includeSparkVersion(v, sv) % "compile"
          includeClassifier(m, Option(c))
        case PATTERN_MODULEID_1(g, "", a, v, c) =>
          val m = g % a % includeSparkVersion(v, sv) % "compile"
          includeClassifier(m, Option(c))
        case PATTERN_MODULEID_2(g, "%", a, v, p, c) =>
          val m = g %% a % includeSparkVersion(v, sv) % p
          includeClassifier(m, Option(c))
        case PATTERN_MODULEID_2(g, "", a, v, p, c) =>
          val m = g % a % includeSparkVersion(v, sv) % p
          includeClassifier(m, Option(c))
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(g % a % includeSparkVersion(v, sv) % "compile")
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
        case PATTERN_MODULEID_1(g, "", a, v, _) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = parsePartialExclude(a)))
        case PATTERN_MODULEID_1(g, "%", a, v, _) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = a+"_2.10"))
        case PATTERN_COORDINATE_1(g, a, v) =>
          Some(ExclusionRule(organization = parsePartialExclude(g), name = parsePartialExclude(a)))
        case _ =>
          None
      }
    }
  }

  def resolve(includes: Seq[ModuleID], exclusions: Seq[ExclusionRule] = Nil, scalaVersion:String="2.10")
      (implicit _resolvers: Seq[Resolver], repo: java.io.File):Try[List[String]] = {
    val logger = new SbtLoggerSlf4j(org.slf4j.LoggerFactory.getLogger("SBT downloads"))
    logger.log(sbt.Level.Debug, "> includes: " + includes)
    logger.log(sbt.Level.Debug, "> exclusions: " + exclusions)
    logger.log(sbt.Level.Debug, "> repo: " + repo.getPath)
    val resolvers = Resolver.file("local-repo", repo / "local")(Resolver.ivyStylePatterns) +: _resolvers
    logger.log(sbt.Level.Debug, "> resolvers: " + resolvers)
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
      "org.scala-lang" % "scala" % scalaVersion % "compile",
      ModuleInfo("dl deps"),
      deps,
      Set.empty,
      scala.xml.NodeSeq.Empty,
      Seq(Compile, Test, Runtime),
      None,
      Some(new IvyScala(
        scalaFullVersion = scalaVersion,
        scalaBinaryVersion = cross.CrossVersionUtil.binaryScalaVersion(scalaVersion),
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

    val files = Try {
      val report: UpdateReport = IvyActions.update(module, config, logger)
      logger.log(sbt.Level.Info, report.toString)
      report.allFiles
    }
    files match {
      case scala.util.Failure(t) => logger.trace(t)
      case _ =>
    }
    val newJars = files.map(_.map(_.getPath).toSet.toList)
    newJars
  }

  def parse(cp: String, sv:String) = {
    val lines = cp.trim().split("\n").toList.map(_.trim()).filter(_.length > 0).toSet.toSeq
    val includes = lines.map(v => Deps.parseInclude(v, sv)).collect { case Some(x) => x }
    val excludes = lines.map(v => Deps.parseExclude(v)).collect { case Some(x) => x }
    (includes, excludes)
  }

  def script(cp: String, resolvers: List[Resolver], repo: java.io.File, sparkVersion:String): Try[List[String]] = {
    val (includes, excludes) = parse(cp, sparkVersion)
    Deps.resolve(includes, excludes)(resolvers, repo)
  }

}

object CustomResolvers extends java.io.Serializable {
  // enable S3 handlers
  fm.sbt.S3ResolverPlugin


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
