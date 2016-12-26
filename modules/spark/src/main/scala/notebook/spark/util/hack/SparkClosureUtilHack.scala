package org.apache.spark.util.notebook.hack

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

trait SparkClosureUtilHack {
  
  private val CC = org.apache.spark.util.ClosureCleaner

  def appendTo: String => Unit = println

  def clean(o:Object) = {
    CC.clean(o, java.lang.Boolean.TRUE, java.lang.Boolean.TRUE)
    // if we reach this point... it's good!
    "Object cleaned and serializable!"
  }

  // open the logger from the closure cleaner
  private val logMethod = CC.getClass.getDeclaredMethod("log")
  logMethod.setAccessible(true)
  private val logSlf4J = logMethod.invoke(CC)
  require(logSlf4J.isInstanceOf[org.slf4j.Logger])
  private val loggerInF = logSlf4J.getClass.getDeclaredField("logger")
  loggerInF.setAccessible(true)
  private val loggerIn = loggerInF.get(logSlf4J)
  require(loggerIn.isInstanceOf[org.apache.log4j.Logger])
  private val logger = loggerIn.asInstanceOf[org.apache.log4j.Logger]

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
  private val ulappender = new HackAppender()
  ulappender.setThreshold(org.apache.log4j.Level.DEBUG)
  ulappender.activateOptions()
  logger.addAppender(ulappender)

  private val restoreLogger = {
    val level = logger.getLevel
    () => {
      logger.removeAppender(ulappender)
      logger.setLevel(level)
    }
  }

  logger.setLevel(org.apache.log4j.Level.DEBUG)

  lazy val restore = restoreLogger()
}