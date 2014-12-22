/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook

import java.util.concurrent.ConcurrentHashMap
import java.security.SecureRandom

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.slf4j.LoggerFactory
import org.apache.commons.codec.binary.Hex

import rx.lang.scala._

import play.api.libs.json._
//import play.api.libs.functional.syntax._


private object JSBusState  {
  private val events = mutable.ArrayBuffer.empty[(String, JsValue)]
  val log = LoggerFactory.getLogger(getClass())

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
    log.debug("Sending %s to %s"format( value, id))
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
  val log = LoggerFactory.getLogger(getClass())

  private[notebook] def forwardClientUpdateMessage(obsId: String, newValue: JsValue) = idToSubject.get(obsId).map(x => {
    println("obs: " + obsId)
    println("x: " + x)
    println("newValue: " + newValue)
    x.onJsReceived(newValue)
  })

  // TODO: How do these things get disposed? Need a notice from Javascript to Scala when an id is disposed, then we dispose all subscriptions (onComplete?)
  private val idToSubject: scala.collection.concurrent.Map[String, ValueConnection] = new scala.collection.concurrent.TrieMap[String, ValueConnection]()

  class ValueConnection extends Connection[JsValue] {
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
    val observable:Observable[JsValue] = new WrappedObservable[JsValue](subject)

    val id = newID
    var current:JsValue  = null


    def onJsReceived(v: JsValue) {
      println(">>><<< : " + v)
      subject.onNext(v)
    }
  }

  def createConnection = {
    val cxn = new ValueConnection
    idToSubject += cxn.id -> cxn
    cxn
  }

  override def toString = "JSBus"
}
