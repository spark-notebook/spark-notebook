package notebook

import java.security.SecureRandom

import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import play.api.libs.json._
import rx.lang.scala._

import scala.collection.mutable

private object JSBusState {
  private val events = mutable.ArrayBuffer.empty[(String, JsValue)]
  val log = LoggerFactory.getLogger(getClass)

  @volatile private var publishCallback: (String, JsValue) => Unit = null

  def setPublisher(callback: (String, JsValue) => Unit) {
    log.debug("Setting publisher")
    events.synchronized {
      publishCallback = callback
      for ((id, value) <- events) {
        log.debug("Dequeuing %s to %s".format(value, id))
        callback(id, value)
      }
      events.clear()
    }
  }

  def publish(id: String, value: JsValue) {
    if (publishCallback == null) {
      events.synchronized {
        if (publishCallback == null) {
          log.debug("Queuing %s to %s".format(value, id))
          events += ((id, value))
          return
        }
      }
    }
    publishCallback(id, value)
  }
}

object JSBus {
  private[this] val random = new SecureRandom

  protected[this] def newID = {
    val bytes = new Array[Byte](16)
    random.nextBytes(bytes)
    val id = "anon" + new String(Hex.encodeHex(bytes))
    id
  }

  protected[this] def send(id: String, value: JsValue) {
    JSBusState.publish(id, value)
  }

  val log = LoggerFactory.getLogger(getClass)

  private[notebook] def forwardClientUpdateMessage(obsId: String,
    newValue: JsValue) = idToSubject.get(obsId).map(_.onJsReceived(newValue))

  // TODO: How do these things get disposed? Need a notice from Javascript to Scala when an id is disposed, then we dispose all subscriptions (onComplete?)
  private val idToSubject: scala.collection.concurrent.Map[String, ValueConnection] = new scala.collection.concurrent.TrieMap[String, ValueConnection]()

  class ValueConnection(val id:String = newID) extends Connection[JsValue] {
    val observer = new ConcreteObserver[JsValue] {
      // Called by extenral parties
      override def onNext(arg: JsValue) {
        //        val wantUpdate = synchronized {
        //          // Prevent echos by only sending changes
        //          if (current == null || current != args) {
        //            current = args
        //            true
        //          }
        //          else {
        //            false
        //          }
        //        }
        //        if (wantUpdate)
        send(id, arg)
      }
    }

    private[this] val subject = Subject[JsValue]()
    val observable: Observable[JsValue] = new WrappedObservable[JsValue](subject)

    var current: JsValue = null


    def onJsReceived(v: JsValue) {
      //println(">>><<< : " + v)
      subject.onNext(v)
    }
  }

  def createConnection:ValueConnection = createConnection(newID)
  def createConnection(id:String):ValueConnection = {
    val cxn = new ValueConnection(id)
    idToSubject += cxn.id -> cxn
    cxn
  }

  override def toString = "JSBus"
}
