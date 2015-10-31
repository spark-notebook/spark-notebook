package notebook

import akka.actor.{Deploy, _}
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration._

import org.slf4j.LoggerFactory

import play.api.libs.iteratee._


class ObsWebSocketService(system: ActorSystem, val channel: Concurrent.Channel[JsValue],
  remoteDeployFuture: Future[Deploy]) {

  val obsActor = system.actorOf(Props(new LocalActor))

  class LocalActor extends Actor with ActorLogging {
    var remote: ActorRef = null

    override def preStart() {
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      remote = context.actorOf(Props[ObsServiceRemoteActor].withDeploy(remoteDeploy))
    }

    def receive = {
      case msg@ObservableBrowserToVM(id, newValue) =>
        remote ! msg
      case ObservableVMToBrowser(id, value) =>
        val respJson = Json.obj("id" -> id, "new_value" -> value)
        //println(s"Pushing respJson: $respJson")

        channel push respJson
    }
  }

}


class ObsServiceRemoteActor extends Actor with ActorLogging {
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
