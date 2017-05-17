package com.datafellas.g3nerator.model

import com.typesafe.config.Config
import notebook.io.ConfigUtils._

import scala.util.Try

/**
  * Credentials for basic authentication
  *
  * @param username
  * @param password
  */
case class Credentials(username: String, password: String)
object Credentials {
  val Username = "username"
  val Password = "password"

  implicit object credentialRender extends Renderable[Credentials] {
    def render(obj: Credentials, prefix: String) = {
      val entries = List(
        StringConfig(Credentials.Username, obj.username),
        StringConfig(Credentials.Password, obj.password)
      )
      Renderable.render(entries, prefix)
    }
  }

  implicit object credentialsParser extends ConfigParser[Credentials] {
    def parseConfig(config: Config): Try[Credentials] = {
      for {
        u <- config.tryGetString(Username)
        p <- config.tryGetString(Password)
      } yield (Credentials(u, p))
    }
  }

}

