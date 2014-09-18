package com.bwater.notebook
package client

import akka.actor.{ActorLogging, Props, Actor}
import kernel._
import java.io.File

import org.apache.spark.repl.HackSparkILoop

/**
 * Author: Ken
 */
sealed trait CalcRequest
case class ExecuteRequest(counter: Int, code: String) extends CalcRequest
case class CompletionRequest(line: String, cursorPosition: Int) extends CalcRequest
case class ObjectInfoRequest(objName: String) extends CalcRequest
case object SparkClassServerUri extends CalcRequest
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
 * @param compilerArgs Command line arguments to pass to the REPL compiler
 */
class ReplCalculator(initScripts: List[String], compilerArgs: List[String]) extends Actor with akka.actor.ActorLogging {
  private lazy val repl = new Repl(compilerArgs)

  // Make a child actor so we don't block the execution on the main thread, so that interruption can work
  private val executor = context.actorOf(Props(new Actor {
    def receive = {
      case ExecuteRequest(_, code) =>
        val (result, _) = repl.evaluate(code, msg => sender ! StreamResponse(msg, "stdout"))

        result match {
          case Success(result)     => sender ! ExecuteResponse(result.toString)
          case Failure(stackTrace) => sender ! ErrorResponse(stackTrace, false)
          case Incomplete          => sender ! ErrorResponse("", true)
        }
    }
  }))

  override def preStart() {
    log.info("ReplCalculator preStart")

    val dummyScript = () => s"""val dummy = ();\n"""
    val SparkHookScript = () => s"""val _5C4L4_N0T3800K_5P4RK_HOOK = "${repl.classServerUri.get}";\n"""

    def eval(script: () => String):Unit = {
      val sc = script()
      if (sc.trim.length > 0) {
      val (result, _) = repl.evaluate(sc)
        result match {
          case Failure(str) =>
            log.error("Error in init script: \n%s".format(str))
          case _ =>
            if (log.isDebugEnabled) log.debug("\n" + sc)
            log.info("Init script processed successfully")
        }
        result
      }
    }

    for (script <- (dummyScript :: SparkHookScript :: initScripts.map(x => () => x)) ) {
      eval(script)
    }
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
          // facility gone
          // repl.interrupt()

        case req @ ExecuteRequest(_, code) => executor.forward(req)

        case SparkClassServerUri =>
          repl.interp match {
            case i:HackSparkILoop => sender ! Some(i.intp.classServer.uri)
            case _ => sender ! None
          }

        case CompletionRequest(line, cursorPosition) =>
          val (matched, candidates) = repl.complete(line, cursorPosition)
          sender ! CompletionResponse(cursorPosition, candidates, matched)

        case ObjectInfoRequest(objName) =>
          val completions = repl.objectInfo(objName)

          val resp = if (completions.length == 0) {
            ObjectInfoResponse(false, objName, "", "")
          } else {
            ObjectInfoResponse(true, objName, completions.mkString("\n"), "")
          }

          sender ! resp
      }
  }
}
