/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor.{ActorRef, Actor}
import org.slf4j.LoggerFactory

/**
* Actor that handles updates from a client and triggers the proper Scala EventStreams and
* vice versa.
*
* Only one of these Handlers is intended to exist in a VM at a time.  On construction, it will
* set the JSBus publisher to itself.
*/
class ObservableHandler(proxy: ActorRef) extends Actor {

  JSBusState.setPublisher((id, value) => proxy ! ObservableVMToBrowser(id, value))

  val log = LoggerFactory.getLogger(getClass())

  log.info("Obs handler is ready")

  def receive = {
    case ObservableBrowserToVM(obsId, newValue) =>
      try {
        log.info("Forwarding message to " + obsId)
        JSBus.forwardClientUpdateMessage(obsId, newValue)
      } catch {
        case e: Exception => e.printStackTrace()
      }
  }

}