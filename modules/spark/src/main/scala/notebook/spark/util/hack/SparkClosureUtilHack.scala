package org.apache.spark.util.notebook.hack

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

trait SparkClosureUtilHack {
  
  private val CC = org.apache.spark.util.ClosureCleaner

  def appendTo: String => Unit = println


  private val YIPPEE = "Object cleaned and serializable!"
  def clean(o:Object) = {
    CC.clean(o, java.lang.Boolean.TRUE, java.lang.Boolean.TRUE)
    // if we reach this point... it's good!
    YIPPEE
  }

  private val logger:org.apache.log4j.Logger = {
    // open the logger from the closure cleaner
    val logMethod = CC.getClass.getDeclaredMethod("log")
    logMethod.setAccessible(true)
    val logSlf4J = logMethod.invoke(CC)
    require(logSlf4J.isInstanceOf[org.slf4j.Logger])
    val loggerInF = logSlf4J.getClass.getDeclaredField("logger")
    loggerInF.setAccessible(true)
    val loggerIn = loggerInF.get(logSlf4J)
    require(loggerIn.isInstanceOf[org.apache.log4j.Logger])
    val logger = loggerIn.asInstanceOf[org.apache.log4j.Logger]
    logger
  }

  // define appender to show the cleaning logs
  class HackAppender extends AppenderSkeleton {
    override def append(event:LoggingEvent) {
      appendTo(event.getMessage.toString)
    }
    override def close() {
    }
    override def requiresLayout() = {
      false;
    }
  }
  // create appender added to logger and the capacity to restore the initial state
  private val hackAppender = new HackAppender()
  hackAppender.setThreshold(org.apache.log4j.Level.DEBUG)
  hackAppender.activateOptions()
  logger.addAppender(hackAppender)

  private val restoreLogger = {
    val level = logger.getLevel
    () => {
      logger.removeAppender(hackAppender)
      logger.setLevel(level)
    }
  }

  logger.setLevel(org.apache.log4j.Level.DEBUG)

  lazy val restore = restoreLogger()
}