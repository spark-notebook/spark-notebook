package com.bwater.notebook

import scala.concurrent._
import akka.actor._
import scala.concurrent.duration._
import unfiltered.netty.websockets.WebSocket
import akka.actor.Deploy
import net.liftweb.json._, JsonDSL._

/**
 * Author: Ken
 */
class ObsWebSocketService(system: ActorSystem, val webSock: WebSocket, remoteDeployFuture: Future[Deploy]) {

  val obsActor = system.actorOf(Props(new LocalActor))

  class LocalActor extends Actor with ActorLogging   {
    var remote: ActorRef = null

    override def preStart() {
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      remote = context.actorOf(Props[ObsServiceRemoteActor].withDeploy(remoteDeploy))
    }

    def receive = {
      case msg@ObservableBrowserToVM(id, newValue) =>
        remote ! msg
      case ObservableVMToBrowser(id, value) =>
        val respJson = ("id" -> id) ~ ("new_value" -> value)
        webSock.send(pretty(render(respJson)))
    }
  }

}



class ObsServiceRemoteActor extends Actor with ActorLogging   {
  override def preStart() {
    JSBusState.setPublisher((id, value) => self ! ObservableVMToBrowser(id, value))
  }

  def receive = {
    case ObservableBrowserToVM(id, newValue) => JSBus.forwardClientUpdateMessage(id, newValue)
    case msg: ObservableVMToBrowser => context.parent ! msg
  }
}
