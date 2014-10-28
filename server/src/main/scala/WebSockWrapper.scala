package notebook
package server

import unfiltered.netty.websockets.WebSocket
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native._
import java.util.UUID
import notebook.util.Logging

/**
 * Author: Ken
 */

trait WebSockWrapper {
  def send(header: JValue, session: JValue, msgType: String, content: JValue)
}

class WebSockWrapperImpl(sock: WebSocket) extends WebSockWrapper with Logging {
  private def send(msg: String) {
    logTrace("Sending " + msg)
    sock.send(msg)
  }

  def send(header: JValue, session: JValue, msgType: String, content: JValue) {
    val respJson = ("parent_header" -> header) ~
      ("msg_type" -> msgType) ~
      ("msg_id" -> UUID.randomUUID().toString) ~
      ("content" -> content) ~
      ("header" -> ("username" -> "kernel") ~
        ("session" -> session) ~
        ("msg_id" -> UUID.randomUUID().toString) ~
        ("msg_type" -> msgType))

    send(prettyJson(renderJValue(respJson)))
  }
}
