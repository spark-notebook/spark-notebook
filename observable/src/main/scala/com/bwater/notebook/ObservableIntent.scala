/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import kernel.remote.VMManager
import unfiltered.netty.websockets
import unfiltered.request.{Seg, Path}
import akka.actor.{Props, Actor, ActorRef, ActorRefFactory}
import websockets._
import net.liftweb.json.JsonAST.{JString, JField}
import net.liftweb.json._, JsonDSL._
import org.slf4j.LoggerFactory

class ObservableIntent(vmManager: ActorRef, system: ActorRefFactory) {

  private val router = system.actorOf(Props[Router])
  val log = LoggerFactory.getLogger(getClass())

  val webSocketIntent: websockets.Intent = {
    case req @ Path(Seg("observable" :: contextId :: Nil)) => {
      case Open(socket) =>
        log.info("Opening observable WebSocket")
        system.actorOf(Props(new Actor {

          router ! Router.Put(contextId, context.self)

          val clientToVM = context.actorOf(Props(new GuardedActor {
            def guard = for {
              handler <- getType[ActorRef]
            } yield {
              case msg:ObservableClientChange =>
                handler ! msg
            }
          }).withDispatcher("akka.actor.default-stash-dispatcher"), "clientToVM")

          locally { // the locally actually matters; prevents serializability shenanigans
            val vmToClient = context.actorOf(Props(new Actor {
              def receive = {
                case ObservableUpdate(obsId, newValue) =>
                  val respJson = ("id" -> obsId) ~ ("new_value" -> newValue)
                  socket.send(pretty(render(respJson)))
              }
            }), "vmToClient")
  
            vmManager.tell(VMManager.Spawn(contextId, Props(new ObservableHandler(vmToClient))), clientToVM)
          }

          def receive = {
            case occ: ObservableClientChange => clientToVM.forward(occ)
          }
        }))


      case Message(_, Text(msg)) =>

        val json = parse(msg)

        for (JField("id", JString(id)) <- json;
             JField("new_value", value) <- json) {
          router ! Router.Forward(contextId, ObservableClientChange(id, value))
        }

      case Close(_) =>
        router ! Router.Remove(contextId)

      case Error(s, e) =>
    }
  }
}
