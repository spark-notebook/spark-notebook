package notebook.front.widgets

import notebook._
import notebook.front._
import play.api.libs.json._

class Button(text:String)(implicit val codec: Codec[JsValue, Double]) extends Widget with SingleConnector[Double] {
  lazy val toHtml =
    <button  type="button" class="btn btn-xs" data-bind="click: clicked, fireChange: true">
      {scopedScript(
      """
        |req( ['observable', 'knockout'],
        |  function (O, ko) {
        |    ko.applyBindings({
        |      clicks: O.makeObservable(clicksId),
        |      clicked : function() {
        |          this.clicks(Math.random());
        |      }
        |    },
        |    this);
        |  }
        |);""".stripMargin,
      Json.obj("clicksId" -> dataConnection.id)
    )}
    {text}
    </button>
}