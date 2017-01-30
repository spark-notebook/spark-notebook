package notebook.front.widgets

import play.api.libs.json._

import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.Utils.Defaults.DEFAULT_MAX_POINTS
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

trait Texts extends Generic with Utils {

  def text(value: String) = html(xml.Text(value))

  def text(value: Connection[String], style: Connection[String] = Connection.just("")) = {
    val _currentValue = JSBus.createConnection
    val stringCodec:Codec[JsValue, String] = formatToCodec(None)(implicitly[Format[String]])
    val currentValue = _currentValue biMap stringCodec
    currentValue <-- value

    val _currentStyle = JSBus.createConnection
    val currentStyle = _currentStyle biMap stringCodec
    currentStyle <-- style

    html(<p data-bind="text: value, style: style">{
      scopedScript(
        """
          |req(
          |['observable', 'knockout'],
          |function (O, ko) {
          |  ko.applyBindings({
          |      value: O.makeObservable(valueId),
          |      style: O.makeObservable(styleId)
          |    },
          |    this
          |  );
          |});
        """.stripMargin,
        Json.obj("valueId" -> _currentValue.id, "styleId" -> _currentStyle.id)
      )}</p>)
  }

  class OutDiv(initialValue: Option[String]=None) extends SingleConnectedWidget[String] {
    implicit val codec:Codec[JsValue, String] = formatToCodec(None)(Format.of[String])

    lazy val toHtml = <p data-bind="text: value">{
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
                 "initialValue" -> (""+initialValue.getOrElse("")))
      )}</p>
  }

  def out = new OutDiv()
  def outWithInitialValue(initialValue:String) = new OutDiv(Some(initialValue))
}