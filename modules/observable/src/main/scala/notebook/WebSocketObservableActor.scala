package notebook

import akka.actor._
import play.api.libs.iteratee._
import play.api.libs.json._

object WebSocketObservableActor {
  def props(out: Concurrent.Channel[JsValue], contextId: String)(implicit
    system: ActorSystem): ActorRef = system.actorOf(Props(new WebSocketObservableActor(out, contextId)))
}

class WebSocketObservableActor(channel: Concurrent.Channel[JsValue], val contextId: String)(implicit system: ActorSystem) extends Actor {

  var service: Option[ObsWebSocketService] = None

  KernelManager.get(contextId) foreach { kernel =>
    service match {
      case None => service = Some(new ObsWebSocketService(system, channel, kernel.remoteDeployFuture))
      case Some(s) => s
    }
  }

  def receive = {
    case m@("add", channel: Concurrent.Channel[_ /*JsValue*/]) =>
      service foreach (_.obsActor ! m)

    case m@("remove", channel: Concurrent.Channel[_ /*JsValue*/]) =>
      service foreach (_.obsActor ! m)

    case msg: JsValue =>
      msg.validate[ObservableBrowserToVM] match {
        case s: JsSuccess[ObservableBrowserToVM] =>
          service foreach (_.obsActor ! s.get)
        case e: JsError =>
          throw new RuntimeException("Errors: " + JsError.toFlatJson(e).toString())
      }
  }
}