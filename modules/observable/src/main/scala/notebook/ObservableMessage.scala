package notebook

import play.api.libs.json._

trait ObservableMessage

case class ObservableBrowserToVM(id: String, new_value: JsValue) extends ObservableMessage

object ObservableBrowserToVM {
  implicit val format = Json.format[ObservableBrowserToVM]
}

case class ObservableVMToBrowser(id: String, update: JsValue) extends ObservableMessage

object ObservableVMToBrowser {
  implicit val format = Json.format[ObservableVMToBrowser]
}
