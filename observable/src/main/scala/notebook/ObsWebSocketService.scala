package notebook

import scala.concurrent._
import akka.actor._
import scala.concurrent.duration._
import unfiltered.netty.websockets.WebSocket
import akka.actor.Deploy
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native._


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
        webSock.send(prettyJson(renderJValue(respJson)))
    }
  }

}



class ObsServiceRemoteActor extends Actor with ActorLogging   {
  override def preStart() {
    JSBusState.setPublisher((id, value) => self ! ObservableVMToBrowser(id, value))
  }

  def receive = {
    case ObservableBrowserToVM(id, newValue) =>
      JSBus.forwardClientUpdateMessage(id, newValue)
    case msg: ObservableVMToBrowser =>
      context.parent ! msg
  }
}
