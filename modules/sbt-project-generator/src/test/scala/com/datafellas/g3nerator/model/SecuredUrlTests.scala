package com.datafellas.g3nerator.model

import com.typesafe.config.ConfigFactory
import notebook.io.ConfigurationMissingException
import org.scalatest.{Matchers, OptionValues, TryValues, WordSpec}

import scala.collection.JavaConverters._

class SecuredUrlTests extends WordSpec with Matchers with TryValues with OptionValues {

  "SecuredUrl" should {
    val url = "http://some-server:1234"
    val user = "someUser"
    val pwd = "somePwd"

    "parse a valid configuration with URL and Credentials" in {
      val conf = ConfigFactory.parseMap(Map(
        "url"-> url,
        "authentication.username" -> user,
        "authentication.password" -> pwd).asJava
      )
      val securedUrl = ConfigParser.parse[SecuredUrl](conf)
      val res = securedUrl.success.value
      res.url should be (URL(url))
      res.credentials.value should be (Credentials(user,pwd))
    }

    "parse a valid configuration with URL and no Credentials" in {
      val conf = ConfigFactory.parseMap(
        Map("url"-> url).asJava
      )
      val securedUrl = ConfigParser.parse[SecuredUrl](conf)
      val res = securedUrl.success.value
      res.url should be (URL(url))
      res.credentials should be ('empty)
    }

    "fail parsing an  invalid configuration without URL and valid credentials" in {
      val conf = ConfigFactory.parseMap(Map(
        "badurl"-> url,
        "authentication.username" -> user,
        "authentication.password" -> pwd).asJava
      )
      val securedUrl = ConfigParser.parse[SecuredUrl](conf)
      val res = securedUrl.failure
      res.exception shouldBe a[ConfigurationMissingException]
      res.exception.asInstanceOf[ConfigurationMissingException].key should be ("url")
    }

    "fail parsing an  invalid configuration with valid URL and invalid credentials" in {
      val conf = ConfigFactory.parseMap(Map(
        "url"-> url,
        "authentication.foosername" -> user,
        "authentication.password" -> pwd).asJava
      )
      val securedUrl = ConfigParser.parse[SecuredUrl](conf)
      val res = securedUrl.failure
      res.exception shouldBe a[ConfigurationMissingException]
      res.exception.asInstanceOf[ConfigurationMissingException].key should be ("username")
    }
  }
}
