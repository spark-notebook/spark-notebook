package utils

import notebook.io.ConfigurationMissingException
import play.api.Configuration

import scala.util.{Failure, Success, Try}


object ConfigurationUtils {

  implicit class ConfigurationOps(val baseConfiguration:Configuration) extends AnyVal {

    def tryGetString(key: String) : Try[String] = {
      baseConfiguration.getString(key).map(Success(_)).getOrElse(Failure(new ConfigurationMissingException(key)))
    }

    def getMandatoryConfig(key: String) : Configuration = {
      tryGetConfig(key).get
    }

    def tryGetConfig(key: String) : Try[Configuration] = {
      baseConfiguration.getConfig(key).map(Success(_)).getOrElse(Failure(new ConfigurationMissingException(key)))
    }

  }

}
