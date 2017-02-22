package notebook.spark.util

import scala.reflect.runtime.universe
import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.spi.LoggingEvent

/**
 * A closure cleaning util which allows accessing the log output by Spark's `ClosureCleaner`.
 * This object is responsible to evict unnecessary objects from the closure context of an object or function.
 * Also it will test if it is serializable.
 * However, there is no clear information we can get out naturally out of this helper, unless we can have 
 * access to the logs issued internally. This gives clear information about what's going on, and specially 
 * *wrong* when it's the case.
 *
 * It Registers the given `appendTo` function to the logger of the Spark's ClosureCleaner. 
 * The default is to `println` to stdout.
 * Example: in a spark notebook, we can use a `widgets.ul` in a cell and use the object's method `append` to
 * print in the notebook the logs from the cleaner.
 **/
class SparkClosureUtil(appendTo: String => Unit = println) 
  extends org.apache.spark.util.notebook.hack.SparkClosureUtilHack {
}