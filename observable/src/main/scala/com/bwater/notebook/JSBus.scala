/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json.JsonAST.{JArray, JString, JInt, JValue}
import org.apache.commons.codec.binary.Hex
import java.security.SecureRandom
import collection.mutable
import rx.subjects.Subject
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import rx.operators.OperationMap
import org.slf4j.LoggerFactory
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonDSL._

private object JSBusState  {
  private val events = mutable.ArrayBuffer.empty[(String, JValue)]
  val log = LoggerFactory.getLogger(getClass())

  @volatile private var publishCallback: (String, JValue) => Unit = null

  def setPublisher(callback: (String, JValue) => Unit) {
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

  def publish(id: String, value: JValue) {
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
    "anon" + new String(Hex.encodeHex(bytes))
  }

  protected[this] def send(id: String, value: JValue) {
    JSBusState.publish(id, value)
  }
  val log = LoggerFactory.getLogger(getClass())

  private[notebook] def forwardClientUpdateMessage(obsId: String, newValue: JValue) = idToSubject.get(obsId).map(x => {
    x.onJsReceived(newValue)
  })

  // TODO: How do these things get disposed? Need a notice from Javascript to Scala when an id is disposed, then we dispose all subscriptions (onComplete?)
  private val idToSubject: collection.mutable.ConcurrentMap[String, ValueConnection] = new ConcurrentHashMap[String, ValueConnection]()

  class ValueConnection extends Connection[JValue] {
    val observer = new ConcreteObserver[JValue] {
      // Called by extenral parties
      override def onNext(arg: JValue) {
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

    private[this] val subject = Subject.create[JValue]
    val observable:Observable[JValue] = new WrappedObservable[JValue](subject)

    val id = newID
    var current:JValue  = null


    def onJsReceived(v: JValue) {
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
