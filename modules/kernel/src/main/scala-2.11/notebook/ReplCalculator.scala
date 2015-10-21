package notebook
package client

import java.io.{File, FileWriter}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.util.{Try, Success=>TSuccess, Failure=>TFailure}

import akka.actor.{Actor, ActorRef, ActorLogging, Props}
import kernel._

import org.sonatype.aether.repository.RemoteRepository

import notebook.OutputTypes._
import notebook.util.{Deps, Match, Repos}
import notebook.front._
import notebook.front.widgets._


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

  private def outputTypesRegex(ctx:String, outputType:String) = s"(?s)^:$ctx\\s*\n(.+)\\s*$$".r → outputType
  private val htmlContext       = outputTypesRegex("html",       `text/html`)
  private val plainContext      = outputTypesRegex("plain",      `text/plain`)
  private val markdownContext   = outputTypesRegex("markdown",   `text/markdown`)
  private val latexContext      = outputTypesRegex("latex",      `text/latex`)
  private val svgContext        = outputTypesRegex("svg",        `image/svg+xml`)
  private val pngContext        = outputTypesRegex("png",        `image/png`)
  private val jpegContext       = outputTypesRegex("jpeg",       `image/jpeg`)
  private val pdfContext        = outputTypesRegex("pdf",        `application/pdf`)
  private val javascriptContext = outputTypesRegex("javascript", `application/javascript`)

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

  var remotes:List[RemoteRepository] = customRepos.getOrElse(List.empty[String]).map(remoreRepo _).map(_._2) ::: List(Repos.central, Repos.oss)

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
    implicit val ec = context.dispatcher

    private var queue:Queue[(ActorRef, ExecuteRequest)] = Queue.empty

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
      case "process-next" =>
        log.debug(s"Processing next asked, queue is ${queue.size} length now")
        if (queue.nonEmpty) { //queue could be empty if InterruptRequest was asked!
          log.debug("Dequeuing execute request current size: " + queue.size)
          queue = queue.dequeue._2
          queue.headOption foreach { case (ref, er) =>
            log.debug("About to execute request from the queue")
            execute(ref, er)
          }
        }

      case er@ExecuteRequest(_, code) if queue.nonEmpty =>
        log.debug("Enqueuing execute request at: " + queue.size)
        queue = queue.enqueue(sender(), er)

      case er@ExecuteRequest(_, code) =>
        log.debug("Enqueuing execute request at: " + queue.size)
        queue = queue.enqueue(sender(), er)
        log.debug("Executing execute request")
        execute(sender(), er)

      case InterruptRequest =>
        log.debug("Interrupting the spark context")
        val thisSender = sender()
        log.debug("Clearing the queue of size " + queue.size)
        queue = scala.collection.immutable.Queue.empty
        repl.evaluate(
          "sparkContext.cancelAllJobs()",
          msg => {
            thisSender ! StreamResponse(msg, "stdout")
          }
        )
    }

    def execute(sender:ActorRef, er:ExecuteRequest):Unit = {
      val (outputType, newCode) = er.code match {
        case remoteRegex(r) =>
          log.debug("Adding remote repo: " + r)
          val (logR, remote) = remoreRepo(r)
          remotes = remote :: remotes
          (`text/plain`, s""" "Remote repo added: $logR!" """)

        case repoRegex(r) =>
          log.debug("Updating local repo: " + r)
          repo = new File(r.trim)
          repo.mkdirs
          (`text/plain`, s""" "Repo changed to ${repo.getAbsolutePath}!" """)

        case dpRegex(cp) =>
          log.debug("Fetching deps using repos: " + remotes.mkString(" -- "))
          val tryDeps = Deps.script(cp, remotes, repo)

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

              (`text/plain`,
              s"""
                 |//updating deps
                 |jars = (${deps.mkString("List(\"", "\",\"", "\")")} ::: jars.toList).distinct.toArray
                 |//restarting spark
                 |reset()
                 |jars.toList
                 """.stripMargin
              )
            case TFailure(ex) =>
              log.error(ex, "Cannot add dependencies")
              (`text/html`, s""" <p style="color:red">${ex.getMessage}</p> """)
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
          (`text/plain`, s"""
            "Classpath CHANGED!"
          """)

        case shRegex(sh) =>
          val ps = "s\"\"\""+sh.replaceAll("\\s*\\|\\s*", "\" #\\| \"").replaceAll("\\s*&&\\s*", "\" #&& \"")+"\"\"\""

          (`text/plain`, s"""
             |import sys.process._
             |$ps.!!
              """.stripMargin.trim
          )

        case sqlRegex(n, sql) =>
          log.debug(s"Received sql code: [$n] $sql")
          val qs = "\"\"\""
          val name = Option(n).map(nm => s"@transient val $nm = ").getOrElse ("")
          (`text/plain`,
            s"""
            import notebook.front.widgets.Sql
            import notebook.front.widgets.Sql._
            ${name}new Sql(sqlContext, s$qs$sql$qs)
            """
          )

        case htmlContext._1(content)        =>
          val ctx = htmlContext._2
          val c = content.toString.replaceAll("\"", "&quot;")
          (ctx, " scala.xml.XML.loadString(s\"\"\""+c+"\"\"\") ")

        case plainContext._1(content)       =>
          val ctx = plainContext._2
          val c = content.toString.replaceAll("\"", "\\\\\\\"")
          (ctx, " s\"\"\""+c+"\"\"\" ")

        case markdownContext._1(content)    =>
          val ctx = markdownContext._2
          val c = content.toString.replaceAll("\"", "\\\\\\\"")
          (ctx, " s\"\"\""+c+"\"\"\" ")

        case latexContext._1(content)       =>
          val ctx = latexContext._2
          val c = content.toString.replaceAll("\"", "\\\\\\\"")
          (ctx, " s\"\"\""+c+"\"\"\" ")

        case svgContext._1(content)         =>
          val ctx = svgContext._2
          val c = content.toString.replaceAll("\"", "&quot;")
          (ctx, " scala.xml.XML.loadString(s\"\"\""+c+"\"\"\") ")

        case pngContext._1(content)         =>
          val ctx = pngContext._2
          (ctx, content.toString)

        case jpegContext._1(content)        =>
          val ctx = jpegContext._2
          (ctx, content.toString)

        case pdfContext._1(content)         =>
          val ctx = pdfContext._2
          (ctx, content.toString)

        case javascriptContext._1(content)  =>
          val ctx = javascriptContext._2
          val c = content.toString//.replaceAll("\"", "\\\"")
          (ctx, " s\"\"\""+c+"\"\"\" ")

        case whatever => (`text/html`, whatever)
      }

      val start = System.currentTimeMillis
      val thisSelf = self
      val thisSender = sender
      val result = scala.concurrent.Future {
        // this future is required to allow InterruptRequest messages to be received and process
        // so that spark jobs can be killed and the hand given back to the user to refine their tasks
        val result = repl.evaluate(newCode, msg => thisSender ! StreamResponse(msg, "stdout"))
        val d = toCoarsest(Duration(System.currentTimeMillis - start, MILLISECONDS))
        (d, result._1)
      }

      result foreach {
        case (timeToEval, Success(result)) =>
          thisSender ! ExecuteResponse(outputType, result.toString, timeToEval)
        case (timeToEval, Failure(stackTrace)) =>
          thisSender ! ErrorResponse(stackTrace, incomplete = false)
        case (timeToEval, notebook.kernel.Incomplete) =>
          thisSender ! ErrorResponse("Incomplete (hint: check the parenthesis)", incomplete = true)
      }

      result onComplete {
        _ => thisSelf ! "process-next"
      }
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