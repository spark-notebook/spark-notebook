package com.datafellas.g3nerator.model

import com.typesafe.config.Config

import scala.util.Try

/**
  * Some machinery to enable class to provide their parse logic in a consistent way
  */
trait ConfigParser[T] {
  def parseConfig(config:Config):Try[T]
}

object ConfigParser {
  def parse[T:ConfigParser](config:Config): Try[T] = {
    val parser = implicitly[ConfigParser[T]]
    parser.parseConfig(config)
  }
}