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

    var channels:List[Concurrent.Channel[JsValue]] = List(channel)

    override def preStart() {
      val remoteDeploy = Await.result(remoteDeployFuture, 2 minutes)
      remote = context.actorOf(Props[ObsServiceRemoteActor].withDeploy(remoteDeploy))
    }

    def receive = {
      case m@("add", channel: Concurrent.Channel[JsValue]) =>
        //println(s"Adding channel $channel to channels $channels")
        channels = channel :: channels

      case m@("remove", channel: Concurrent.Channel[JsValue]) =>
        //println(s"Removing channel $channel to channels $channels")
        channels = channels.filter(_ != channel)
        if (channels.isEmpty) {
          println(s"WebSocketObservableActor: no more channels are attached, but not killing the actor")
          //sender() ! akka.actor.PoisonPill
        }

      case msg@ObservableBrowserToVM(id, newValue) =>
        remote ! msg

      case ObservableVMToBrowser(id, value) =>
        val respJson = Json.obj("id" -> id, "new_value" -> value)
        //println(s"Pushing respJson: $respJson")
        //println(s"Channels are: $channels")

        channels foreach (channel => channel push respJson)
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
