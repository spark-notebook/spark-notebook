package com.datafellas.g3nerator.model

import java.net.{URL => JURL}

import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}
import notebook.io.ConfigUtils._
import notebook.io.ConfigurationMissingException

/**
  * An endpoint that requires credentials
  */
case class SecuredUrl(url: JURL, credentials: Option[Credentials]){
  def toCurlCredentials:String = credentials.map(cred => s"-u '${cred.username}:${cred.password}'").getOrElse("")
}

object SecuredUrl {

  val Url = "url"
  val Authentication = "authentication"

  implicit object securedUrlRender extends Renderable[SecuredUrl] {
    def render(obj: SecuredUrl, prefix: String) = {
      obj.credentials.foldLeft(Renderable.render(StringConfig(Url, obj.url.toString), prefix))((url, cred) =>
        url + "\n" + Renderable.render(cred, prefix + "." + Authentication))
    }
  }

  implicit object securedUrlParser extends ConfigParser[SecuredUrl] {
    def parseConfig(config: Config): Try[SecuredUrl] = {
      for {
        strUrl <- config.tryGetString(Url)
        url <- Try {
          URL(strUrl)
        }
        credConfig <- config.tryGetPath(Authentication).map(Some(_))
          .recoverWith { case ex: ConfigurationMissingException => Success(None) }
        credentials <- credConfig.map(config => ConfigParser.parse[Credentials](config)) match {
          case Some(Success(cred)) => Success(Some(cred))
          case Some(Failure(ex)) => Failure(ex)
          case None => Success(None)
        }
      } yield SecuredUrl(url, credentials)
    }
  }

}
