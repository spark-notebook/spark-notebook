/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.kernel

import java.io.File
import com.typesafe.config.Config
import collection.JavaConversions._
import com.typesafe.config.ConfigException


object ConfigUtils {
  
  implicit def configToRichConfig(config: Config) = new EasyConfig(config)

  class EasyConfig(config: Config) {
    def get(key: String) = if (config.hasPath(key)) Some(config.getString(key)) else None

    def getArray(key: String): Option[List[String]] = {
      if (config.hasPath(key)) {
        Some(configPathAsList(config, key))
      } else None
    }
    
    def getMem(key: String) = get(key) map parseMem
  }
  
  private def configPathAsList(c: Config, path: String) = {
    scala.util.control.Exception.allCatch.either(c.getStringList(path).toList) match {
          /* Read scalar value as list for backwards compat with config that has been converted from scalar to list. */
          case Left(_: ConfigException.WrongType) => List(c.getString(path))
          case Left(e) => throw e
          case Right(v) => v
        }
  }
  
  private val MemSpec = """\s*(\d+)\s*([kKmMgG]?)""".r

  private def parseMem(memString: String) = {
    val MemSpec(mem, unit) = memString
    mem.toLong << (unit match {
      case ""  =>  0
      case "k" => 10
      case "m" => 20
      case "g" => 30
    })
  }
}
