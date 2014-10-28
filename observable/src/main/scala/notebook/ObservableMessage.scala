/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook

import org.json4s._

trait ObservableMessage
case class ObservableBrowserToVM(id: String, newValue: JValue) extends ObservableMessage
case class ObservableVMToBrowser(id: String, update: JValue) extends ObservableMessage

