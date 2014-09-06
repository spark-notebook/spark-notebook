/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook
package kernel
package remote

import akka.actor._
import akka.remote.transport.Transport.{InboundAssociation, AssociationEvent}
import akka.remote.{DisassociatedEvent, RemoteScope}
import pfork.ProcessFork
import com.typesafe.config.ConfigFactory
import collection.JavaConversions._
import akka.actor.Deploy
import org.apache.commons.io.FileUtils
import java.io.File
import org.slf4j.LoggerFactory
import org.apache.log4j.{Logger, PropertyConfigurator}

class SingleVM(fork: ProcessFork[RemoteProcess], location: File) extends Actor with ActorLogging {

  def this() = this(new ProcessFork[RemoteProcess], new File("."))

  import SingleVM._

  def startVM(port: Int): ProcessFork.ProcessKiller = {
    //TODO: cookie file should only be written once.
    val akkaCookieFile = new File(location, ".akka-cookie")
    for (cookie <- AkkaConfigUtils.requiredCookie(context.system.settings.config)) {
      FileUtils.writeStringToFile(akkaCookieFile, cookie)
    }
    fork.execute(port.toString, negotiator.path.toStringWithAddress(externalAddress(context.system)), akkaCookieFile.getAbsolutePath)
  }

  val negotiator = context.actorOf(Props(new Actor {

    private[this] var address: Address = _


    def waiting: Receive = {
      case RemoteProcessReady(a) =>
        address = a
        context.become(ready)
    }

    def ready: Receive = {
      case Spawn(props) =>
        sender ! context.actorOf(Props(new CrashResilientActor(props.withDeploy(Deploy(scope = RemoteScope(address))))))

      case DisassociatedEvent(_, a, _) =>
        if (address == a) {
          procKiller = startVM(address.port.get)
          address = null
          context.become(waiting)
        }
    }

    def receive = waiting

    override def postStop() {
      val a = address
      if (a != null) {
        log.info("Shutting down remote VM: %s".format(a))
        context.system.actorOf(Props(new Actor with ActorLogging { log.info("Shutting down VM"); sys.exit(0); def receive = Actor.emptyBehavior }).withDeploy(Deploy(scope = RemoteScope(a))))
      } else {
        log.info("Forcibly shutting down remote VM.")
        val killer = procKiller
        if (killer != null) killer()
      }
    }

  }).withDispatcher("akka.actor.default-stash-dispatcher"))

  context.system.eventStream.subscribe(negotiator, classOf[DisassociatedEvent])
  private[this] var procKiller = startVM(0)

  def receive = {
    case spawn: Spawn => negotiator.forward(spawn)
  }

}



object SingleVM {
  case class Spawn(props: Props)

  case class RemoteProcessReady(address: Address)

  private class CrashResilientActor(props: Props) extends Actor {

    val negotiator = context.actorOf(Props(new Actor with Stash {

      private[this] var remote = context.actorOf(props)

      def waiting: Receive = {
        case InboundAssociation(handle) =>
          if (remote.path.address == handle.remoteAddress) {
            remote = context.actorOf(props)
            unstashAll()
            context.become(forwarding)
          }
        case Fwd(_) => stash()
      }

      def forwarding: Receive = {
        case DisassociatedEvent(local, remoteAddress, _) =>
          if (remote.path.address == remoteAddress) {
            context.become(waiting)
          }
        case Fwd(msg) =>
          remote.forward(msg)
      }

      def receive = forwarding

    }).withDispatcher("akka.actor.default-stash-dispatcher"))

    context.system.eventStream.subscribe(negotiator, classOf[InboundAssociation])
    context.system.eventStream.subscribe(negotiator, classOf[DisassociatedEvent])

    private case class Fwd(msg: Any)

    def receive = {
      case msg => negotiator.forward(Fwd(msg))
    }
  }

  def externalAddress(system: ActorSystem) = system.asInstanceOf[ExtendedActorSystem].provider.getExternalAddressFor(Address("akka", "", "", 0)).get
}

class RemoteProcess(port: String, parentPath: String, cookieFile: String, configPath: String) {

  def this(port: String, parentPath: String, cookieFile: String) = this(port, parentPath, cookieFile, "kernel.conf")

  def this(port: String, parentPath: String) = this(port, parentPath, "")

  locally {
    //TODO: This doesn't really belong here?
    PropertyConfigurator.configure(getClass().getResource("/log4j.subprocess.properties"))
  }

  val system = {
    val cookie = Some(new File(cookieFile)).filter(_.exists).map(FileUtils.readFileToString)
    val baseConfig = ConfigFactory.load(configPath)
    val config = cookie map { AkkaConfigUtils.requireCookie(baseConfig, _) } getOrElse baseConfig
    ActorSystem("RemoteProcess", ConfigFactory.parseMap(Map("akka.remote.netty.port" -> port)).withFallback(config))
  }

  val log = LoggerFactory.getLogger(getClass())

  if (log.isTraceEnabled) {
    system.eventStream.subscribe(system.actorOf(Props(new Actor {
      def receive = {
        case x => log.trace("Event stream msg: " + x) //Don't use actor logging, messages routed back here...
      }
    })), classOf[AnyRef])
  }

  log.info("Spawned VM is ready!")
  system.actorFor(parentPath) ! SingleVM.RemoteProcessReady(SingleVM.externalAddress(system))
}
