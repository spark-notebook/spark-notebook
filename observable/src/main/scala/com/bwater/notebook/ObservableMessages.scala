/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json._

// js -> scala
case class ObservableClientChange(id: String, newValue: JValue)

// scala -> js
case class ObservableUpdate(id: String, update: JValue)
