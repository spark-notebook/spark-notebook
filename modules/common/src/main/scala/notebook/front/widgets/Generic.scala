package notebook.front.widgets

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import magic._

trait Generic {
  def html(html: NodeSeq): Widget = new SimpleWidget(html)

  def reactiveHtml(initialValue: String) = new SingleConnectedWidget[String] {
    implicit val codec:Codec[JsValue, String] = formatToCodec(None)(Format.of[String])

    lazy val toHtml = <div data-bind="html: value">{
      scopedScript(
        """
            |req(
            |['observable', 'knockout'],
            |function (O, ko) {
            |  ko.applyBindings({
            |      value: O.makeObservable(valueId, initialValue)
            |    },
            |    this
            |  );
            |});
        """.stripMargin,
        Json.obj("valueId" -> dataConnection.id,
                 "initialValue" -> initialValue)
      )}</div>
  }
}