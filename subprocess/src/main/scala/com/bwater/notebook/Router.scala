/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor._
import collection.mutable
import akka.actor.Terminated
import scala.Some

class Router extends Actor with ActorLogging {

  import Router._

  private val actors = mutable.Map.empty[Any, ActorRef]
  private val keys = mutable.Map.empty[ActorRef, mutable.Set[Any]]

  private val negotiator = context.actorOf(Props(new Actor {
    def receive = {
      case Forward(key, msg) => actors.get(key) match {
        case Some(actor) => actor.forward(msg)
        case None => log.warning("Dropping message " + msg + " because no actor was available to receive it")
      }
      case Put(key, actorRef) =>
        actors.put(key, actorRef) foreach context.stop
        keys.getOrElseUpdate(actorRef, mutable.Set.empty[Any]) += key

      case Remove(key) =>
        actors.remove(key) match {
          case Some(actor) =>
            keys get actor foreach { _ -= key }
            context.stop(actor)
            log.debug("Stopped actor for key: %s".format(key))
          case None => log.info("Actor with id " + key + " not found, skipping remove")
        }
      case Terminated(actorRef) => {
        keys.remove(actorRef) foreach { _ foreach actors.remove }
      }
    }
  }))

  def receive = {
    case msg: RouterMessage => negotiator forward msg
  }
}

object Router {
  sealed trait RouterMessage
  case class Forward(key: Any, msg: Any) extends RouterMessage
  case class Put(key: Any, actorRef: ActorRef) extends RouterMessage
  case class Remove(key: Any) extends RouterMessage
}