package com.datafellas.g3nerator.model

import java.net.{URL => JURL}

object URL {

  def apply(url: String): JURL = new JURL(url)

  implicit class URLOps(val url: JURL) extends AnyVal {
    def / (path: String): JURL = {
      val trailingBaseSlash = if (url.getPath.lastOption != Some('/')) new JURL(url, url.getPath + "/") else url
      val noLeadingSlash = if (path.headOption == Some('/')) path.drop(1) else path
      new JURL(trailingBaseSlash , noLeadingSlash)
    }
  }

}

