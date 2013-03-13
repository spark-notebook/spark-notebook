/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.util

import org.slf4j.LoggerFactory

trait Logging {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  protected[this] def logInfo(messageGenerator: =>String) { if (log.isInfoEnabled) log.info(messageGenerator) }
  protected[this] def logInfo(messageGenerator: =>String, e: Throwable) { if (log.isInfoEnabled)log.info(messageGenerator, e) }

  protected[this] def logDebug(messageGenerator: =>String) {if (log.isDebugEnabled) log.debug(messageGenerator) }
  protected[this] def logDebug(messageGenerator: =>String, e: Throwable) {if (log.isDebugEnabled) log.debug(messageGenerator, e) }

  protected[this] def logTrace(messageGenerator: =>String) { if (log.isTraceEnabled) log.trace(messageGenerator) }
  protected[this] def logTrace(messageGenerator: =>String, e: Throwable) { if (log.isTraceEnabled)log.info(messageGenerator, e) }
  
  protected[this] def logError(messageGenerator: =>String) { log.error(messageGenerator) }
  protected[this] def logError(messageGenerator: =>String, e: Throwable) { log.error(messageGenerator, e) }
  
  protected[this] def logWarn(messageGenerator: =>String) { log.warn(messageGenerator) }
  protected[this] def logWarn(messageGenerator: =>String, e: Throwable) { log.warn(messageGenerator, e) }

  
  protected[this] def ifDebugEnabled(f: => Unit) = if (log.isDebugEnabled()) f 
  protected[this] def ifTraceEnabled(f: => Unit) = if (log.isTraceEnabled()) f 
}