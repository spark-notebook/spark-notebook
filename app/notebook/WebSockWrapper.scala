package notebook
package server

import java.util.UUID

import play.api.libs.iteratee._
import play.api.libs.json._

import notebook.util.Logging

/**
 * Author: Ken
 */

trait WebSockWrapper {
  def send(header: JsValue, session: JsValue, msgType: String, content: JsValue)
}

class WebSockWrapperImpl(sock: Concurrent.Channel[JsValue]) extends WebSockWrapper with Logging {
  private def send(msg: JsValue) {
    logTrace("Sending " + msg)
    sock.push(msg)
  }

  def send(header: JsValue, session: JsValue, msgType: String, content: JsValue) {
    val respJson = Json.obj(
      "parent_header" -> header,
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
