package notebook.io

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{ExecutionContext, Future}
import notebook.util.Logging

trait Configurable[T] {
  def apply(config: Config = ConfigFactory.empty())(implicit ec:ExecutionContext) : Future[T]
}

class ConfigurationMissingException(val key: String) extends Exception(s"Key missing: [$key]") with Logging {
  // the 'key' value is oftentime lost (then this causes other exception), let's at least print it
  logWarn(s"ConfigurationMissingException: key missing: $key")
}

class ConfigurationCorruptException(msg: String) extends Exception(msg)
