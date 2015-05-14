package notebook
package client

import java.io.{File, FileWriter}

import scala.util.{Try, Success=>TSuccess, Failure=>TFailure}

import akka.actor.{ActorLogging, Props, Actor}
import kernel._

import org.sonatype.aether.repository.RemoteRepository

import notebook.util.{Deps, Match, Repos}
import notebook.front._
import notebook.front.widgets._

sealed trait CalcRequest
case class ExecuteRequest(counter: Int, code: String) extends CalcRequest
case class CompletionRequest(line: String, cursorPosition: Int) extends CalcRequest
case class ObjectInfoRequest(objName: String, position:Int) extends CalcRequest
case object InterruptRequest extends CalcRequest

sealed trait CalcResponse
case class StreamResponse(data: String, name: String) extends CalcResponse
case class ExecuteResponse(html: String) extends CalcResponse
case class ErrorResponse(message: String, incomplete: Boolean) extends CalcResponse

// CY: With high probability, the matchedText field is the segment of the input line that could
// be sensibly replaced with (any of) the candidate.
// i.e.
//
// input: "abc".inst
// ^
// the completions would come back as List("instanceOf") and matchedText => "inst"
//
// ...maybe...
case class CompletionResponse(cursorPosition: Int, candidates: Seq[Match], matchedText: String)

/*
name
call_def
init_definition
definition
call_docstring
init_docstring
docstring
*/
case class ObjectInfoResponse(found: Boolean, name: String, callDef: String, callDocString: String)

/**
 * @param initScripts List of scala source strings to be executed during REPL startup.
 * @param customSparkConf Map configuring the notebook (spark configuration).
 * @param compilerArgs Command line arguments to pass to the REPL compiler
 */
class ReplCalculator(
  customLocalRepo:Option[String],
  customRepos:Option[List[String]],
  customDeps:Option[List[String]],
  customImports:Option[List[String]],
  customSparkConf:Option[Map[String, String]],
  initScripts: List[(String, String)],
  compilerArgs: List[String]
) extends Actor with akka.actor.ActorLogging {

  private val repoRegex = "(?s)^:local-repo\\s*(.+)\\s*$".r
  private val remoteRegex = "(?s)^:remote-repo\\s*(.+)\\s*$".r
  private val authRegex = """(?s)^\s*\(([^\)]+)\)\s*$""".r
  private val credRegex = """"([^"]+)"\s*,\s*"([^"]+)"""".r //"

  private val cpRegex = "(?s)^:cp\\s*(.+)\\s*$".r
  private val dpRegex = "(?s)^:dp\\s*(.+)\\s*$".r
  private val sqlRegex = "(?s)^:sql(?:\\[([a-zA-Z0-9][a-zA-Z0-9]*)\\])?\\s*(.+)\\s*$".r
  private val shRegex = "(?s)^:sh\\s*(.+)\\s*$".r

  private def remoreRepo(r:String):(String, RemoteRepository) = {
    val id::tpe::url::rest = r.split("%").toList
    val (username, password):(Option[String],Option[String]) = rest.headOption.map { auth =>
      auth match {
        case authRegex(usernamePassword)   =>
          val (username, password) = usernamePassword match { case credRegex(username, password) => (username, password) }
          val u = if (username.startsWith("$")) sys.env.get(username.tail).get else username
          val p = if (password.startsWith("$")) sys.env.get(password.tail).get else password
          (Some(u), Some(p))
        case _                             => (None, None)
      }
    }.getOrElse((None, None))
    val rem = Repos(id.trim,tpe.trim,url.trim,username,password)
    val logR = r.replaceAll("\"", "\\\\\"")
    (logR, rem)
  }

  var remotes:List[RemoteRepository] = List(Repos.central, Repos.oss) ::: customRepos.getOrElse(List.empty[String]).map(remoreRepo _).map(_._2)

  var repo:File = customLocalRepo.map(x => new File(x)).getOrElse{
    val tmp = new File(System.getProperty("java.io.tmpdir"))

    val snb = new File(tmp, "spark-notebook")
    if (!snb.exists) snb.mkdirs

    val aether = new File(snb, "aether")
    if (!aether.exists) aether.mkdirs

    val r = new File(aether, java.util.UUID.randomUUID.toString)
    if (!r.exists) r.mkdirs

    r
  }

  def codeRepo = new File(repo, "code")

  val (depsJars, depsScript):(List[String],(String, ()=>String)) = customDeps.map { d =>
    val customDeps = d.mkString("\n")
    val deps = Deps.script(customDeps, remotes, repo).toOption.getOrElse(List.empty[String])
    (deps, ("deps", () => s"""
                    |val CustomJars = ${ deps.mkString("Array(\"", "\",\"", "\")") }
                    |
                    """.stripMargin))
  }.getOrElse((List.empty[String], ("deps", () => "val CustomJars = Array.empty[String]\n")))



  ("deps", () => customDeps.map { d =>
    val customDeps = d.mkString("\n")

    val deps = Deps.script(customDeps, remotes, repo).toOption.getOrElse(List.empty[String])

    (deps, s"""
    |val CustomJars = ${ deps.mkString("Array(\"", "\",\"", "\")") }
    |
    """.stripMargin)
  }.getOrElse((List.empty[String], "val CustomJars = Array.empty[String]\n")))

  val ImportsScripts = ("imports", () => customImports.map(_.mkString("\n") + "\n").getOrElse("\n"))

  private var _repl:Option[Repl] = None

  private def repl:Repl = _repl getOrElse {
    val r = new Repl(compilerArgs, depsJars)
    _repl = Some(r)
    r
  }

  // +/- copied of https://github.com/scala/scala/blob/v2.11.4/src%2Flibrary%2Fscala%2Fconcurrent%2Fduration%2FDuration.scala
  final def toCoarsest(d:FiniteDuration): String = {

    def loop(length: Long, unit: TimeUnit, acc:String): String = {

      def coarserOrThis(coarser: TimeUnit, divider: Int) = {
        if (length == divider)
          loop(1, coarser, acc)
        else if (length < divider)
          FiniteDuration(length, unit).toString + " " + acc
        else {
          val _acc = if (length % divider == 0) {
            acc
          } else {
            FiniteDuration(length % divider, unit).toString + " " + acc
          }
          loop(length / divider, coarser, _acc)
        }
      }

      unit match {
        case DAYS         => d.toString + " " + acc
        case HOURS        => coarserOrThis(DAYS, 24)
        case MINUTES      => coarserOrThis(HOURS, 60)
        case SECONDS      => coarserOrThis(MINUTES, 60)
        case MILLISECONDS => coarserOrThis(SECONDS, 1000)
        case MICROSECONDS => coarserOrThis(MILLISECONDS, 1000)
        case NANOSECONDS  => coarserOrThis(MICROSECONDS, 1000)
      }
    }

    if (d.unit == DAYS || d.length == 0) d.toString
    else loop(d.length, d.unit, "").trim
  }

  // Make a child actor so we don't block the execution on the main thread, so that interruption can work
  private val executor = context.actorOf(Props(new Actor {
    def eval(b: => String, notify:Boolean=true)(success: => String = "", failure: String=>String=(s:String)=>"Error evaluating " + b + ": "+ s) {
      repl.evaluate(b)._1 match {
        case Failure(str) =>
          if (notify) {
            eval(s"""
            """,false)()
          }
          log.error(failure(str))
        case _ =>
          if (notify) {
            eval(s"""
            """,false)()
          }
          log.info(success)
      }
    }

    def receive = {
      case ExecuteRequest(_, code) =>
        val (timeToEval, result) = {
          val newCode =
            code match {
              case remoteRegex(r) =>
                log.debug("Adding remote repo: " + r)
                val (logR, remote) = remoreRepo(r)
                remotes = remote :: remotes
                s""" "Remote repo added: $logR!" """

              case repoRegex(r) =>
                log.debug("Updating local repo: " + r)
                repo = new File(r.trim)
                repo.mkdirs
                s""" "Repo changed to ${repo.getAbsolutePath}!" """

              case dpRegex(cp) =>
                log.debug("Fetching deps using repos: " + remotes.mkString(" -- "))
                eval("""
                """, false)()
                val tryDeps = Deps.script(cp, remotes, repo)
                eval("""
                """, false)()

                tryDeps match {
                  case TSuccess(deps) =>
                    eval("""
                      sparkContext.stop()
                    """)(
                      "CP reload processed successfully",
                      (str:String) => "Error in :dp: \n%s".format(str)
                    )
                    val (_r, replay) = repl.addCp(deps)
                    _repl = Some(_r)
                    preStartLogic()
                    replay()

                    s"""
                      |//updating deps
                      |jars = (${ deps.mkString("List(\"", "\",\"", "\")") } ::: jars.toList).distinct.toArray
                      |//restarting spark
                      |reset()
                      |jars.toList
                    """.stripMargin
                  case TFailure(ex) =>
                    log.error(ex, "Cannot add dependencies")
                    s""" <p style="color:red">${ex.getMessage}</p> """
                }

              case cpRegex(cp) =>
                val jars = cp.trim().split("\n").toList.map(_.trim()).filter(_.size > 0)
                repl.evaluate("""
                  sparkContext.stop()
                """)._1 match {
                  case Failure(str) =>
                    log.error("Error in :cp: \n%s".format(str))
                  case _ =>
                    log.info("CP reload processed successfully")
                }
                val (_r, replay) = repl.addCp(jars)
                _repl = Some(_r)
                preStartLogic()
                replay()
                s"""
                  "Classpath CHANGED!"
                """

              case shRegex(sh) =>
                val ps = "s\"\"\""+sh.replaceAll("\\s*\\|\\s*", "\" #\\| \"").replaceAll("\\s*&&\\s*", "\" #&& \"")+"\"\"\""

                s"""
                | import sys.process._
                | <pre>{$ps !!}</pre>
                """.stripMargin.trim

              case sqlRegex(n, sql) =>
                log.debug(s"Received sql code: [$n] $sql")
                val qs = "\"\"\""
                //if (!sqlGen.parts.isEmpty) {
                  val name = Option(n).map(nm => s"@transient val $nm = ").getOrElse ("")
                  val c = s"""
                    import notebook.front.widgets.Sql
                    import notebook.front.widgets.Sql._
                    ${name}new Sql(sqlContext, s${qs}${sql}${qs})
                  """
                  c
              case _ => code
            }

          val start = System.currentTimeMillis
          val thisSender = sender
          val result = scala.concurrent.Future { repl.evaluate(newCode, msg => thisSender ! StreamResponse(msg, "stdout")) }
          val d = toCoarsest(Duration(System.currentTimeMillis - start, MILLISECONDS))
          (d, result.map(_._1))
        }

        val thisSender = sender
        result map {
          case Success(result)     => thisSender ! ExecuteResponse(result.toString + s"\n <div class='pull-right text-info'><small>$timeToEval</small></div>")
          case Failure(stackTrace) => thisSender ! ErrorResponse(stackTrace, false)
          case kernel.Incomplete   => thisSender ! ErrorResponse("", true)
        }
      case InterruptRequest =>
        val thisSender = sender
        repl.evaluate("sparkContext.cancelAllJobs()", msg => thisSender ! StreamResponse(msg, "stdout"))
    }
  }))

  def preStartLogic() {
    log.info("ReplCalculator preStart")

    val dummyScript = ("dummy", () => s"""val dummy = ();\n""")
    val SparkHookScript = ("class server", () => s"""@transient val _5C4L4_N0T3800K_5P4RK_HOOK = "${repl.classServerUri.get}";\n""")

    val CustomSparkConfFromNotebookMD = ("custom conf", () => s"""
      |@transient val _5C4L4_N0T3800K_5P4RK_C0NF:Map[String, String] = Map(
      |  ${customSparkConf.getOrElse(Map.empty[String, String]).map{ case (k, v) => "( \"" + k + "\"  → \"" + v + "\" )"}.mkString(",") }
      |)\n
      """.stripMargin
    )

    def eval(script: () => String):Unit = {
      val sc = script()
      log.debug("script is :\n" + sc)
      if (sc.trim.length > 0) {
        val (result, _) = repl.evaluate(sc)
        result match {
          case Failure(str) =>
            log.error("Error in init script: \n%s".format(str))
          case _ =>
            if (log.isDebugEnabled) log.debug("\n" + sc)
            log.info("Init script processed successfully")
        }
      } else ()
    }

    val allInitScrips: List[(String, () => String)] = dummyScript :: SparkHookScript :: depsScript :: ImportsScripts :: CustomSparkConfFromNotebookMD :: initScripts.map(x => (x._1, () => x._2))
    for ((name, script) <- allInitScrips) {
      log.info(s" INIT SCRIPT: $name")
      eval(script)
    }
  }

  override def preStart() {
    preStartLogic()
    super.preStart()
  }

  override def postStop() {
    log.info("ReplCalculator postStop")
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.info("ReplCalculator preRestart " + message)
    reason.printStackTrace()
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable) {
    log.info("ReplCalculator postRestart")
    reason.printStackTrace()
    super.postRestart(reason)
  }

  def receive = {
    case msgThatShouldBeFromTheKernel =>

      msgThatShouldBeFromTheKernel match {
        case InterruptRequest =>
          executor.forward(InterruptRequest)

        case req @ ExecuteRequest(_, code) => executor.forward(req)

        case CompletionRequest(line, cursorPosition) =>
          val (matched, candidates) = repl.complete(line, cursorPosition)
          sender ! CompletionResponse(cursorPosition, candidates, matched)

        case ObjectInfoRequest(code, position) =>
          val completions = repl.objectInfo(code, position)

          val resp = if (completions.length == 0) {
            ObjectInfoResponse(false, code, "", "")
          } else {
            ObjectInfoResponse(true, code, completions.mkString("\n"), "")
          }

          sender ! resp
      }
  }
}