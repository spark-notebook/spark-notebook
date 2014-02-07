/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import unfiltered.netty.websockets
import unfiltered.request.{Seg, Path}
import akka.actor.{ActorSystem, ActorRef, ActorRefFactory}
import websockets._
import net.liftweb.json.JsonAST.{JString, JField}
import net.liftweb.json._

class ObservableIntent(system: ActorSystem) {

  val kernelIdToObsService = collection.mutable.Map[String, ObsWebSocketService]()

  val webSocketIntent: websockets.Intent = {
    case req @ Path(Seg("observable" :: contextId :: Nil)) => {
      case Open(socket) =>
        for (kernel <- KernelManager.get(contextId)) {

          val service = new ObsWebSocketService(system, socket, kernel.remoteDeployFuture)
          kernelIdToObsService += contextId -> service
        }

      case Message(socket, Text(msg)) =>
        for {
          json <- parseOpt(msg)
          service <- kernelIdToObsService.get(contextId)
          JField("id", JString(id)) <- json
          JField("new_value", value) <- json
        } {
            service.obsActor ! ObservableBrowserToVM(id, value)
        }

      case Close(_) =>
        kernelIdToObsService.remove(contextId)

      case Error(s, e) =>
    }
  }
}
