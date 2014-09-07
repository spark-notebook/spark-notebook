package com.bwater.notebook
package server

import akka.actor._
import scala.concurrent._
import com.bwater.notebook.client._
import net.liftweb.json._
import JsonDSL._
import com.bwater.notebook.client.ObjectInfoRequest
import com.bwater.notebook.client.StreamResponse
import com.bwater.notebook.client.CompletionRequest
import com.bwater.notebook.client.ObjectInfoResponse
import com.bwater.notebook.client.ExecuteRequest
import scala.Some
import com.bwater.notebook.client.CompletionResponse
import com.bwater.notebook.client.ErrorResponse
import com.bwater.notebook.client.ExecuteResponse
import akka.actor.Terminated
import scala.concurrent.duration._

/**
 * Provides a web-socket interface to the Calculator
 */
class CalcWebSocketService(system: ActorSystem, initScripts: List[String], compilerArgs: List[String], remoteDeployFuture: Future[Deploy]) {
  implicit val executor = system.dispatcher

  val ioPubPromise = Promise[WebSockWrapper]
  val shellPromise = Promise[WebSockWrapper]

  val calcActor = system.actorOf(Props( new CalcActor))

  class CalcActor extends Actor with ActorLogging {
    private var currentSessionOperation: Option[ActorRef] = None
    var calculator: ActorRef = null
    var iopub: WebSockWrapper = null
    var shell: WebSockWrapper = null

    private def spawnCalculator() {
      // N.B.: without these local copies of the instance variables, we'll capture all sorts of things in our closure
      // that we don't want, then akka's attempts at serialization will fail and kittens everywhere will cry.
      val kCompilerArgs = compilerArgs
      val kInitScripts = initScripts
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      calculator = context.actorOf(Props(new ReplCalculator(kInitScripts, kCompilerArgs)).withDeploy(remoteDeploy))
    }

    override def preStart() {
      iopub = Await.result(ioPubPromise.future, 2 minutes)
      shell = Await.result(shellPromise.future, 2 minutes)
      spawnCalculator()
    }

    def receive = {
      case InterruptCalculator =>
        for (op <- currentSessionOperation) {
          calculator.tell(InterruptRequest, op)
        }

      case SparkClassServerUri => {
        calculator forward SparkClassServerUri
      }

      case req@SessionRequest(header, session, request) =>
        val operations = new SessionOperationActors(header, session)
        val operationActor = (request: @unchecked) match {
          case ExecuteRequest(counter, code) =>
            iopub.send(header, session, "status", ("execution_state" -> "busy"))
            iopub.send(header, session, "pyin", ("execution_count" -> counter) ~ ("code" -> code))
            operations.singleExecution(counter)

          case _: CompletionRequest =>
            operations.completion

          case _: ObjectInfoRequest =>
            operations.objectInfo
        }
        val operation = context.actorOf(operationActor)
        context.watch(operation)
        currentSessionOperation = Some(operation)
        calculator.tell(request, operation)


      case Terminated(actor) =>
        log.warning("Termination")
        if (actor == calculator) {
          spawnCalculator()
        } else {
          currentSessionOperation = None
        }
    }

    class SessionOperationActors(header: JValue, session: JValue) {
      def singleExecution(counter: Int) = Props(new Actor {
        def receive = {
          case StreamResponse(data, name) =>
            iopub.send(header, session, "stream", ("data" -> data) ~ ("name" -> name))

          case ExecuteResponse(html) =>
            iopub.send(header, session, "pyout", ("execution_count" -> counter) ~ ("data" -> ("text/html" -> html)))
            iopub.send(header, session, "status", ("execution_state" -> "idle"))
            shell.send(header, session, "execute_reply", ("execution_count" -> counter))
            context.stop(self)

          case ErrorResponse(msg, incomplete) =>
            if (incomplete) {
              iopub.send(header, session, "pyincomplete", ("execution_count" -> counter) ~ ("status" -> "error"))
            } else {
              iopub.send(header, session, "pyerr", ("execution_count" -> counter) ~ ("status" -> "error") ~ ("ename" -> "Error") ~ ("traceback" -> Seq(msg)))
            }
            iopub.send(header, session, "status", ("execution_state" -> "idle"))
            shell.send(header, session, "execute_reply", ("execution_count" -> counter))
            context.stop(self)
        }
      })

      def completion = Props(new Actor {
        def receive = {
          case CompletionResponse(cursorPosition, candidates, matchedText) =>
            shell.send(header, session, "complete_reply", ("matched_text" -> matchedText) ~ ("matches" -> candidates.map(_.toJson).toList))
            context.stop(self)
        }
      })

      def objectInfo = Props(new Actor {
        def receive = {
          case ObjectInfoResponse(found, name, callDef, callDocString) =>
            shell.send(
              header,
              session,
              "object_info_reply",
              ("found" -> found) ~
                ("name" -> name) ~
                ("call_def" -> callDef) ~
                ("call_docstring" -> "Description TBD")
            )
            context.stop(self)
        }
      })
    }
  }
}

