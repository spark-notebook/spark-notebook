/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.kernel.remote

import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConversions._


/**
 *
 */

object AkkaConfigUtils {

  private val SecureCookiePath = "akka.remote.netty.secure-cookie"
  private val RequireCookiePath = "akka.remote.netty.require-cookie"

  /** Creates a configuration that requires the specified secure requiredCookie if defined. */
  def requireCookie(baseConfig: Config, cookie: String) =
    ConfigFactory.parseMap(Map(
      SecureCookiePath -> cookie,
      RequireCookiePath -> "on"
    )).withFallback(baseConfig)

  /** If the specified configuration requires a secure requiredCookie, but does not define the requiredCookie value, this generates a new config with an appropriate value. */
  def optSecureCookie(baseConfig: Config, cookie: => String) =
    requiredCookie(baseConfig).map {
      req => if (req.isEmpty) {
        ConfigFactory.parseMap(Map(SecureCookiePath -> cookie)).withFallback(baseConfig)
      } else baseConfig
    } getOrElse baseConfig

  /** Returns the secure requiredCookie value if the specified Config requires their use. */
  def requiredCookie(config: Config) =
    if (config.getBoolean(RequireCookiePath)) {
      Some(config.getString(SecureCookiePath))
    } else None
}
