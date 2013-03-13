/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor.Actor

class EchoActor extends Actor {
  def receive = {
    case x ⇒ sender ! x
  }
}