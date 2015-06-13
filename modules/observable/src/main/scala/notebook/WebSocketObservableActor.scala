package notebook

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

object WebSocketObservableActor {
  def props(out: Concurrent.Channel[JsValue], contextId: String)(implicit
    system: ActorSystem): ActorRef = system.actorOf(Props(new WebSocketObservableActor(out, contextId)))
}

class WebSocketObservableActor(channel: Concurrent.Channel[JsValue], val contextId: String)
    (implicit system: ActorSystem) extends Actor {

  var service: ObsWebSocketService = _

  KernelManager.get(contextId) foreach { kernel =>
    service = new ObsWebSocketService(system, channel, kernel.remoteDeployFuture)
  }

  def receive = {
    case msg: JsValue =>
      msg.validate[ObservableBrowserToVM] match {
        case s: JsSuccess[ObservableBrowserToVM] =>
          service.obsActor ! s.get
        case e: JsError =>
          throw new RuntimeException("Errors: " + JsError.toFlatJson(e).toString())
      }
  }
}