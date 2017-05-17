package com.datafellas.g3nerator.model

import java.net.{URL => JURL}
import java.nio.file.Paths

import org.scalatest.{Matchers, WordSpec}

class URLTests extends WordSpec with Matchers {
  import com.datafellas.g3nerator.model.URL._

  "an URL (wrapper)" should {
    "create an URL from a valid url in a String" in {
      URL("http://host:9876/path") should be (new JURL("http://host:9876/path"))
    }

    "create a new URL from a base host a relative path" in {
      val baseURL = URL("http://host:9876")
      baseURL / "sub/path" should be (new JURL("http://host:9876/sub/path"))
    }

    "create a new URL from a base URL and a relative path" in {
      val baseURL = URL("http://host:9876/path")
      baseURL / "sub/path" should be (new JURL("http://host:9876/path/sub/path"))
    }

    "chain sub paths to create a new URL " in {
      val baseURL = URL("http://host:9876/path")
      baseURL / "sub" / "path" should be (new JURL("http://host:9876/path/sub/path"))
    }

    "allow trailing slash with chainining sub paths to create a new URL " in {
      val baseURL = URL("http://host:9876/path")
      baseURL / "sub/" / "path" should be (new JURL("http://host:9876/path/sub/path"))
    }

    "allow leading slash with chainining sub paths to create a new URL " in {
      val baseURL = URL("http://host:9876/path")
      baseURL / "/sub" / "/path" / "end" should be (new JURL("http://host:9876/path/sub/path/end"))
    }
  }
}