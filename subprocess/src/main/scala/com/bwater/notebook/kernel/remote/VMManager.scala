/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook
package kernel.remote

import akka.actor.{Actor, Props}
import kernel.pfork.ProcessFork
import java.io.File

class VMManager(process: ProcessFork[RemoteProcess]) extends Actor {

  import VMManager._

  private[this] val router = context.actorOf(Props[Router])

  def receive = {
    case Start(key, location) =>
      router ! Router.Put(key, context.actorOf(Props(new SingleVM(process, location))))
    case Spawn(key, props) =>
      router.forward(Router.Forward(key, SingleVM.Spawn(props)))
    case Kill(key) =>
      router ! Router.Remove(key)
  }
}

object VMManager {
  case class Start(key: Any, location: File)
  case class Spawn(key: Any, props: Props)
  case class Kill(key: Any)
}