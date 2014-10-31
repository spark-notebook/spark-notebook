package notebook.front.widgets

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import notebook._, JSBus._

import notebook.front._


class InputBox(initial: String) extends Widget {
  private[this] val connection = JSBus.createConnection
  val currentValue = connection biMap JsonCodec.strings

  currentValue <-- Connection.just(initial)

  lazy val toHtml = <input data-bind="value: value">{
    scopedScript(
      """require( ['observable', 'knockout'],
                  function (Observable, ko) {
                    ko.applyBindings({
                      value: Observable.makeObservable(valueId)
                    }, this);
                  }
                )""",
      ("valueId" -> connection.id)
    )
  }</input>
}