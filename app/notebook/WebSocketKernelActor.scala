package notebook

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.actor._

import play.api.libs.iteratee._
import play.api.libs.json._

import notebook.server._
import notebook.client._

object WebSocketKernelActor {
  def props(channel: Concurrent.Channel[JsValue], pchannel:String, calcService:CalcWebSocketService)(implicit system:ActorSystem):ActorRef =
    system.actorOf(Props(new WebSocketKernelActor(channel, pchannel, calcService)))
}

class WebSocketKernelActor(channel: Concurrent.Channel[JsValue], val pchannel:String, val calcService:CalcWebSocketService)(implicit system:ActorSystem) extends Actor {
  val executionCounter = new AtomicInteger(0)

  if (pchannel == "iopub")
    calcService.ioPubPromise.success(new WebSockWrapperImpl(channel))
  else if (pchannel == "shell")
    calcService.shellPromise.success(new WebSockWrapperImpl(channel))

  def receive = {
    case json:JsValue =>
      //logDebug("Message for " + kernelId + ":" + msg)

      val header   = json \ "header"
      val session  = header \ "session"
      val msgType  = header \ "msg_type"
      val content  = json \ "content"

      msgType match {
        case JsString("execute_request") => {
          val JsString(code) = content \ "code"
          val execCounter = executionCounter.incrementAndGet()
          calcService.calcActor ! SessionRequest(header, session, ExecuteRequest(execCounter, code))
        }

        case JsString("complete_request") => {
          val JsString(line) = content \ "line";
          val JsNumber(cursorPos) = content \ "cursor_pos"
          calcService.calcActor ! SessionRequest(header, session, CompletionRequest(line, cursorPos.toInt))
        }

        case JsString("object_info_request") => {
          val JsString(oname) = content \ "oname"
          calcService.calcActor ! SessionRequest(header, session, ObjectInfoRequest(oname))
        }

        case x => //logWarn("Unrecognized websocket message: " + msg) //throw new IllegalArgumentException("Unrecognized message type " + x)
      }
  }

}