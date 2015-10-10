package notebook.util

import sbt._

class SbtLoggerSlf4j(logger:org.slf4j.Logger) extends BasicLogger {


 def control(event: sbt.ControlEvent.Value,message: => String): Unit = log(Level.Info, message)
 def logAll(events: Seq[LogEvent]): Unit = events foreach (e => log(e))

 // Members declared in sbt.Logger
 def log(level: Level.Value, message: => String): Unit = level match {
  case Level.Debug => logger.debug(message)
  case Level.Info => logger .info(message)
  case Level.Warn => logger .warn(message)
  case Level.Error => logger.error(message)
 }

 def success(message: => String): Unit = log(Level.Info, message)

 def trace(t: => Throwable): Unit = logger.error("Tracing error in SBT", t)
}