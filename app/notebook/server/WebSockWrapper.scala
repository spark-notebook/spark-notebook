package notebook.server

import java.util.UUID

import notebook.util.Logging
import play.api.libs.iteratee._
import play.api.libs.json._

/**
 * Author: Ken
 */

trait WebSockWrapper {

  def session: String

  def send(header: JsValue, session: String, msgType: String, channel: String, content: JsValue)
}

class WebSockWrapperImpl(sock: Concurrent.Channel[JsValue],
  val session: String) extends WebSockWrapper with Logging {
  private def send(msg: JsValue) {
    logTrace("Sending " + msg)
    sock.push(msg)
  }

  val sessionTransformer = (__ \ 'session).json.update(
    Reads.of[JsString].map { case JsString(s) => JsString(session) }
  )

  def injectCurrentSession(header: JsValue): JsValue = {
    val newHeader = header.transform(sessionTransformer)
    newHeader.get
  }

  def send(header: JsValue, __session: String /*ignored in favor of session_id!!!*/ ,
    msgType: String, channel: String, content: JsValue) {
    val respJson = Json.obj(
      "channel" -> channel,
      "parent_header" -> injectCurrentSession(header),
      "msg_type" -> msgType,
      "msg_id" -> UUID.randomUUID().toString,
      "content" -> content,
      "header" -> Json.obj(
        "username" -> "kernel",
        "session" -> session,
        "msg_id" -> UUID.randomUUID().toString,
        "msg_type" -> msgType
      )
    )

    send(respJson)
  }
}
