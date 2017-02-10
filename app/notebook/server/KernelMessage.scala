package notebook.server

import notebook.client.CalcRequest
import play.api.libs.json._

/**
 * Author: Ken
 */
trait KernelMessage
case object RestartKernel extends KernelMessage
case object Shutdown extends KernelMessage


trait CalcServiceMessage
case class Unregister(ws:WebSockWrapper) extends CalcServiceMessage
case class Register(ws:WebSockWrapper) extends CalcServiceMessage

case class SessionRequest(header: JsValue, session: JsValue, kernelRequest: CalcRequest) extends CalcServiceMessage
case object InterruptCalculator extends CalcServiceMessage
case class InterruptCell(cellId: String) extends CalcServiceMessage
case class WebUIReadyNotification(ws:WebSockWrapper)
