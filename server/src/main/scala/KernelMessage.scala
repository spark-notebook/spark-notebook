package notebook
package server

import notebook.client.CalcRequest
import org.json4s._

/**
 * Author: Ken
 */
trait KernelMessage
case object RestartKernel extends KernelMessage
case object Shutdown extends KernelMessage

trait CalcServiceMessage
case class SessionRequest(header: JValue, session: JValue, kernelRequest: CalcRequest) extends CalcServiceMessage
case object InterruptCalculator extends CalcServiceMessage

