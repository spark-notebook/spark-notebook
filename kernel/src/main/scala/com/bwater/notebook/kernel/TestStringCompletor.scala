/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.kernel

import com.bwater.notebook.{Match, StringCompletor}

class TestStringCompletor extends StringCompletor {
  def complete(stringToComplete: String) = (stringToComplete, if (stringToComplete.toLowerCase.startsWith("usa")) Seq("usacpi", "usangdp").map(Match(_, Map("Location" -> "InflationEstimate/master"))) else Seq())
}
