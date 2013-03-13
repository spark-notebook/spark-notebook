package com.bwater.notebook
package server

import akka.actor._
import client._
import net.liftweb.json._
import JsonDSL._
import java.io.File
import org.apache.commons.io.FileUtils
import com.bwater.notebook.util.Logging

/**
 * Handles all kernel requests.
 */
class Kernel(initScripts: List[String], compilerArgs: List[String], spawner: (Props, ActorRef) => Unit) extends GuardedActor with akka.actor.ActorLogging {

  def guard = {
    case class CalculatorReady(calculator: ActorRef)

    def spawnCalculator() {
      // N.B.: without these local copies of the instance variables, we'll capture all sorts of things in our closure
      // that we don't want, then akka's attempts at serialization will fail and kittens everywhere will cry.
      val compilerArgs = this.compilerArgs
      val kInitScripts = initScripts
      
      spawner(Props(new ReplCalculator(kInitScripts, compilerArgs)), context.actorOf(Props(new Actor {
        def receive = {
          case actor: ActorRef =>
            Kernel.this.self ! CalculatorReady(actor)
            context.stop(self)
        }
      })))
    }

    for {
      iopub & shell <- get { case IopubChannel(sock) => sock } & get { case ShellChannel(sock) => sock }
      liveKernel = new LiveKernel(iopub, shell)
      _ = spawnCalculator()
      calculator <- get { case CalculatorReady(calculator) => calculator }
      manager = context.actorOf({ val compilerBugWorkaround /* fixed in 2.10 */ = liveKernel; Props(new compilerBugWorkaround.ExecutionManager(calculator)).withDispatcher("akka.actor.default-stash-dispatcher") })
    } yield {
      case any => manager.forward(any)
    }
  }
}

sealed trait KernelMessage
case class SessionRequest(header: JValue, session: JValue, kernelRequest: CalcRequest) extends KernelMessage
case class IopubChannel(sock: WebSockWrapper) extends KernelMessage
case class ShellChannel(sock: WebSockWrapper) extends KernelMessage
case object InterruptKernel extends KernelMessage

class LiveKernel(iopub: WebSockWrapper, shell: WebSockWrapper) {

  class ExecutionManager(calculator: ActorRef) extends Actor with Stash {

    context.watch(calculator)

    private var currentSessionOperation: Option[ActorRef] = None

    def receive = {
      case InterruptKernel =>
        for (op <- currentSessionOperation) {
          calculator.tell(InterruptRequest, op)
        }
      case req@SessionRequest(header, session, request) =>
        if (currentSessionOperation.isDefined) {
          stash()
        } else {
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
        }

      case Terminated(actor) =>
        if (actor == calculator) {
          unstashAll()
          throw new Exception("Calculator crashed; resetting kernel")
        } else {
          unstashAll()
          currentSessionOperation = None
        }
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