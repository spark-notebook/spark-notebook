package notebook.spark.util

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

class SparkClosureUtil(appendTo: String => Unit = println) {

  // get the closure cleaner
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
  private val module = runtimeMirror.staticModule("org.apache.spark.util.ClosureCleaner")
  private val obj = runtimeMirror.reflectModule(module).instance

  // make the clean method accessible
  private val cleanMethods = obj.getClass.getDeclaredMethods().filter(_.getName == "clean")
  assert(cleanMethods.size == 1)

  def clean(o:Object) = cleanMethods.head.invoke(obj, o, java.lang.Boolean.TRUE, java.lang.Boolean.TRUE)

  // open the logger from the closure cleaner
  private val logMethod = obj.getClass.getDeclaredMethod("log")
  logMethod.setAccessible(true)
  private val logSlf4J = logMethod.invoke(obj)
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