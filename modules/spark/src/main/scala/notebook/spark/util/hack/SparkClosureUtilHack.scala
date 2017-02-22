package org.apache.spark.util.notebook.hack

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

object SparkClosureUtilHack {
  val CLEAN_SUSCCESS = "Object cleaned and serializable!"
}

/*
 * See modules/spark/src/main/scala/notebook/spark/util/SparkClosureUtil.scala
 */
trait SparkClosureUtilHack {
  
  private val CC = org.apache.spark.util.ClosureCleaner

  def appendTo: String => Unit = println

  def clean(o:Object) = {
    CC.clean(o, java.lang.Boolean.TRUE, java.lang.Boolean.TRUE)
    // if we reach this point... it's good!
    SparkClosureUtilHack.CLEAN_SUSCCESS
  }

  private val sparkClosureUtilInternalLogger: org.apache.log4j.Logger = {
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
      false
    }
  }
  // create appender added to logger and the capacity to restore the initial state
  private val hackAppender = new HackAppender()
  hackAppender.setThreshold(org.apache.log4j.Level.DEBUG)
  hackAppender.activateOptions()
  sparkClosureUtilInternalLogger.addAppender(hackAppender)

  private val restoreLogger = {
    val level = sparkClosureUtilInternalLogger.getLevel
    () => {
      sparkClosureUtilInternalLogger.removeAppender(hackAppender)
      sparkClosureUtilInternalLogger.setLevel(level)
    }
  }

  sparkClosureUtilInternalLogger.setLevel(org.apache.log4j.Level.DEBUG)

  lazy val restore = restoreLogger()
}
