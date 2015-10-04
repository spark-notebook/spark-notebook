package notebook.kernel

import akka.actor.{Actor, ActorRef}

class WebSocketAppender extends Actor {

  var ref:Option[ActorRef] = None

  val appender =  new org.apache.log4j.AppenderSkeleton {
                    override def append(event:org.apache.log4j.spi.LoggingEvent) = {
                      //debug --- println("Received in AppenderSkeleton : " + event + " ---  actor ref: " + ref)
                      ref foreach (_ ! event)
                    }
                    override def close(): Unit = ()
                    override def requiresLayout(): Boolean = false
                  }

  def receive = {
    case r:ActorRef =>
      //debug --- println("Received ref in WebSocketAppender : " + r + " ---  current actor ref: " + ref)
      if (ref == None) {
        ref = Some(r)
        org.apache.log4j.Logger.getRootLogger().addAppender(appender)
      } else {
        ref = Some(r)
      }
  }

}