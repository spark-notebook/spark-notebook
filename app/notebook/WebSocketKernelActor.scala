package notebook

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.actor._

import play.api.libs.iteratee._
import play.api.libs.json._

import notebook.server._
import notebook.client._

object WebSocketKernelActor {
  def props(channel: Concurrent.Channel[JsValue], calcService:CalcWebSocketService, session_id:String)(implicit system:ActorSystem):ActorRef =
    system.actorOf(Props(new WebSocketKernelActor(channel, calcService, session_id)))
}

class WebSocketKernelActor(channel: Concurrent.Channel[JsValue], val calcService:CalcWebSocketService, session_id:String)(implicit system:ActorSystem) extends Actor {
  val executionCounter = new AtomicInteger(0)

  val ws = new WebSockWrapperImpl(channel, session_id)
  calcService.register(ws)

  override def postStop = {
    calcService.unregister(ws)
  }

  def receive = {
    case json:JsValue =>
      val header   = json \ "header"
      val session  = header \ "session"
      val msgType  = header \ "msg_type"
      val content  = json \ "content"
      val channel  = json \ "channel"

      msgType match {
        case JsString("kernel_info_request") => {
          ws.send(header, session_id, "info", "shell",
                  Json.obj(
                      "language_info" -> Json.obj(
                        "name"            → "scala",
                        "file_extension"  → "scala",
                        "codemirror_mode" → "text/x-scala"
                      ),
                      "extension" → "scala"
                    )
                  )
        }

        case JsString("execute_request") => {
          val JsString(code) = content \ "code"
          val execCounter = executionCounter.incrementAndGet()
          calcService.calcActor ! SessionRequest(header, session, ExecuteRequest(execCounter, code))
        }

        case JsString("complete_request") => {
          val JsString(line) = content \ "code";
          val JsNumber(cursorPos) = content \ "cursor_pos"
          calcService.calcActor ! SessionRequest(header, session, CompletionRequest(line, cursorPos.toInt))
        }

        case JsString("inspect_request") => {
          val JsString(code) = content \ "code"
          val JsNumber(position) = content \ "cursor_pos"
          val JsNumber(detailLevel) = content \ "detail_level" //0,1,2,3
          calcService.calcActor ! SessionRequest(header, session, ObjectInfoRequest(code, position.toInt))
        }

        case x => //logWarn("Unrecognized websocket message: " + msg) //throw new IllegalArgumentException("Unrecognized message type " + x)
      }
  }

}