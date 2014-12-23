/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook

import play.api.libs.json._
import play.api.libs.functional.syntax._

trait ObservableMessage
case class ObservableBrowserToVM(id: String, new_value: JsValue) extends ObservableMessage
object ObservableBrowserToVM {
  implicit val format = Json.format[ObservableBrowserToVM]
}

case class ObservableVMToBrowser(id: String, update: JsValue) extends ObservableMessage
object ObservableVMToBrowser {
  implicit val format = Json.format[ObservableVMToBrowser]
}
