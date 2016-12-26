package notebook.spark.util

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

class SparkClosureUtil(appendTo: String => Unit = println) 
  extends org.apache.spark.util.notebook.hack.SparkClosureUtilHack {
}