package notebook.front.gadgets

import scala.concurrent.duration._
import scala.util._

import play.api.libs.json._

class Chat() {

  val connection = notebook.JSBus.createConnection("chat")

  connection --> notebook.Connection.fromObserver { msg =>
    connection <-- notebook.Connection.just(msg)
  }

}