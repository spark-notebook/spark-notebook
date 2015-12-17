package notebook.server

import akka.actor.{Terminated, _}
import notebook.client._
import play.api._
import play.api.libs.json.Json.{obj, arr}
import play.api.libs.json._

import scala.concurrent._
import scala.collection.immutable.Queue
import scala.concurrent.duration._

/**
 * Provides a web-socket interface to the Calculator
 */
class CalcWebSocketService(
  system: ActorSystem,
  notebookName:String,
  customLocalRepo: Option[String],
  customRepos: Option[List[String]],
  customDeps: Option[List[String]],
  customImports: Option[List[String]],
  customArgs: Option[List[String]],
  customSparkConf: Option[Map[String, String]],
  initScripts: List[(String, String)],
  compilerArgs: List[String],
  remoteDeployFuture: Future[Deploy],
  tachyonInfo: Option[notebook.server.TachyonInfo]) {

  implicit val executor = system.dispatcher

  val calcActor = system.actorOf(Props(new CalcActor))

  def register(ws: WebSockWrapper) = calcActor ! Register(ws)

  def unregister(ws: WebSockWrapper) = calcActor ! Unregister(ws)

  class CalcActor extends Actor with ActorLogging {
    private var currentSessionOperation: Queue[ActorRef] = Queue.empty
    var calculator: ActorRef = null
    var wss: List[WebSockWrapper] = Nil

    val ws = new {
      def send(header: JsValue, session: JsValue /*ignored*/ , msgType: String, channel: String,
        content: JsValue) = {
        Logger.trace(s"Sending info to websockets $wss in $this")
        wss.foreach { ws =>
          ws.send(header, ws.session, msgType, channel, content)
        }
      }
    }

    private def spawnCalculator() {
      // N.B.: without these local copies of the instance variables, we'll capture all sorts of things in our closure
      // that we don't want, then akka's attempts at serialization will fail and kittens everywhere will cry.
      val kNotebookName = notebookName
      val kCompilerArgs = compilerArgs
      val kCustomLocalRepo = customLocalRepo
      val kCustomRepos = customRepos
      val kCustomDeps = customDeps
      val kCustomImports = customImports
      val kCustomArgs = customArgs

      val tachyon = tachyonInfo.map { info =>
        Map(
          "spark.tachyonStore.url" → info.url.getOrElse(
            "tachyon://" + notebook.share.Tachyon.host + ":" + notebook.share.Tachyon.port
          ),
          "spark.externalBlockStore.url" → info.url.getOrElse(
            "tachyon://" + notebook.share.Tachyon.host + ":" + notebook.share.Tachyon.port
          ),
          "spark.tachyonStore.baseDir" → info.baseDir,
          "spark.externalBlockStore.baseDir" → info.baseDir
        )
      }.getOrElse(Map.empty[String, String])
      val kCustomSparkConf = customSparkConf.map(_ ++ tachyon).orElse(Some(tachyon))
      val kInitScripts = initScripts
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)

      calculator = context.actorOf {
        Props(
          classOf[ReplCalculator],
          kNotebookName,
          kCustomLocalRepo,
          kCustomRepos,
          kCustomDeps,
          kCustomImports,
          kCustomArgs,
          kCustomSparkConf,
          self,
          kInitScripts,
          kCompilerArgs
        ).withDeploy(remoteDeploy)
      }

      context.watch(calculator)
    }

    def receive = {
      case Register(ws: WebSockWrapper) if wss.isEmpty && calculator == null =>
        Logger.info(s"Registering first web-socket ($ws) in service ${this}")
        wss = List(ws)
        Logger.info(s"Spawning calculator in service ${this}")
        spawnCalculator()

      case Register(ws: WebSockWrapper) =>
        Logger.info(s"Registering web-socket ($ws) in service ${this} (current count is ${wss.size})")
        wss = ws :: wss

      case Unregister(ws: WebSockWrapper) =>
        Logger.info(s"UN-registering web-socket ($ws) in service ${this} (current count is ${wss.size})")
        wss = wss.filterNot(_ == ws)

      case InterruptCalculator =>
        Logger.info(s"Interrupting the computations, current is $currentSessionOperation")
        currentSessionOperation.headOption.foreach { op =>
          //cancelling the spark jobs in the first cell
          calculator.tell(InterruptRequest, op)
        }
        if(currentSessionOperation.tail.nonEmpty) {
          //cleaning the other cells
          currentSessionOperation.tail.foreach(_ ! StreamResponse("Previous cell has been interrupted", "stdout"))
        }
        currentSessionOperation = Queue.empty

      case req@SessionRequest(header, session, request) =>
        val operations = new SessionOperationActors(header, session)
        val operationActor = (request: @unchecked) match {
          case ExecuteRequest(counter, code) =>
            ws.send(header, session, "status", "iopub", obj("execution_state" → "busy"))
            ws.send(header, session, "pyin", "iopub", obj("execution_count" → counter, "code" → code))
            operations.singleExecution(counter)

          case _: CompletionRequest =>
            operations.completion

          case _: ObjectInfoRequest =>
            operations.objectInfo
        }
        val operation = context.actorOf(operationActor)
        context.watch(operation)
        currentSessionOperation = currentSessionOperation.enqueue(operation)
        calculator.tell(request, operation)


      case Terminated(actor) =>
        Logger.debug("Termination of op calculator")
        if (actor == calculator) {
          Logger.error(s"Remote calculator ($calculator) has been terminated !!!!!")
          ws.send(
            obj(
              "session" → "ignored"
            ),
            JsNull,
            "status",
            "iopub",
            obj("execution_state" → "dead")
          )
          self ! PoisonPill
        } else {
          if (currentSessionOperation.nonEmpty) {
            currentSessionOperation = currentSessionOperation.dequeue._2
          }
        }

      case event:org.apache.log4j.spi.LoggingEvent =>
        // println("Received log event: " + s"""
        //   > ${event.getLevel}
        //   > ${event.getTimeStamp}
        //   > ${event.getLoggerName}
        //   > ${event.getMessage}
        // """)
        ws.send(
          obj(
            "session" → "ignored"
          ),
          JsNull,
          "log",
          "iopub",
          obj(
            "level"       → event.getLevel.toString,
            "time_stamp"  → event.getTimeStamp,
            "logger_name" → event.getLoggerName,
            "message"     → (""+Option(event.getMessage).map(_.toString).getOrElse("<no-message>")),
            "thrown"      → (if (event.getThrowableStrRep == null) List.empty[String] else event.getThrowableStrRep.toList)
          )
        )
    }

    class SessionOperationActors(header: JsValue, session: JsValue) {
      def singleExecution(counter: Int) = Props(new Actor {
        def receive = {
          case StreamResponse(data, name) =>
            ws.send(header, session, "stream", "iopub", obj("text" → data, "name" → name))

          case ExecuteResponse(outputType, content, time) =>
            ws.send(header, session, "execute_result", "iopub", obj(
              "execution_count" → counter,
              "data" → obj(outputType → content),
              "time" → time
            ))
            ws.send(header, session, "status", "iopub", obj("execution_state" → "idle"))
            ws.send(header, session, "execute_reply", "shell", obj("execution_count" → counter))
            context.stop(self)

          case ErrorResponse(msg, incomplete) =>
            if (incomplete) {
              ws.send(header, session, "error", "iopub", obj(
                "execution_count" → counter,
                "status" → "error",
                "ename" → "Error",
                "traceback" → Seq(msg)
              ))
            } else {
              //already printed by the repl!
              //ws.send(header, session, "error", "iopub", Json.obj("execution_count" → counter,
              // "status" → "error", "ename" → "Error", "traceback" → Seq(msg)))
            }
            ws.send(header, session, "status", "iopub", obj("execution_state" → "idle"))
            ws.send(header, session, "execute_reply", "shell", obj("execution_count" → counter))
            context.stop(self)
        }
      })

      def completion = Props(new Actor {
        def receive = {
          case CompletionResponse(cursorPosition, candidates, matchedText) =>
            ws.send(header, session, "complete_reply", "shell", obj(
              "matched_text" → matchedText,
              "matches" → candidates.map(_.toJsonWithDescription).toList,
              "cursor_start" → (cursorPosition - matchedText.length),
              "cursor_end" → cursorPosition))
            context.stop(self)
        }
      })

      def objectInfo = Props(new Actor {
        def receive = {
          case ObjectInfoResponse(found, name, callDef, callDocString) =>
            ws.send(header, session, "object_info_reply", "shell", obj(
              "found" → found,
              "name" → name,
              "call_def" → callDef,
              "call_docstring" → "Description TBD"
            ))
            context.stop(self)
        }
      })
    }

  }

}
