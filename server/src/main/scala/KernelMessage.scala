package com.bwater.notebook
package server

import com.bwater.notebook.client.CalcRequest
import net.liftweb.json._

/**
 * Author: Ken
 */
trait KernelMessage
case object RestartKernel extends KernelMessage
case object Shutdown extends KernelMessage

trait CalcServiceMessage
case class SessionRequest(header: JValue, session: JValue, kernelRequest: CalcRequest) extends CalcServiceMessage
case object InterruptCalculator extends CalcServiceMessage

