package notebook.front.widgets

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import notebook._, JSBus._

import notebook.front._


class InputBox(initial: String, label:String="") extends Widget with SingleConnector[String] {
  val codec =  JsonCodec.strings

  val id = "input-"+dataConnection.id

  lazy val toHtml = (<label for={id}>{label}</label>) ++ <input id={id} name={id} data-bind="value: value">{
    scopedScript(
      """require( ['observable', 'knockout'],
                  function (Observable, ko) {
                    ko.applyBindings({
                      value: Observable.makeObservable(valueId)
                    }, this);
                  }
                )""",
      ("valueId" -> dataConnection.id)
    )
  }</input>
}