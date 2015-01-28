package notebook.front.widgets

import notebook.front.Widget
import notebook._, JSBus._

import play.api.libs.json._

import rx.Observable

class DropDown[A](options: Seq[A], toString: A=>String = (a:A)=>a.toString) extends Widget  {

  // Just implement the second function here to support option changing in JavaScript
  private lazy val _optionsConnection = JSBus.createConnection
  lazy val optionsConnection = _optionsConnection.biMap[Seq[A]](
    (s: Seq[A]) => JsArray(s.zipWithIndex map { case (opt, i) => Json.obj("text" -> toString(opt), "index" -> i) } toList).asInstanceOf[JsValue],
    (_:JsValue) => Seq[A]()
  )

  private lazy val _selected = JSBus.createConnection
  lazy val selected = _selected.biMap[A](
    (a:A) => JsNumber(options.indexOf(a)).asInstanceOf[JsValue],
    (v:JsValue) => options(v.as[Int])
  )

  optionsConnection <-- Connection.just(options)

  lazy val toHtml =
    <select data-bind="options: options, optionsText: 'text', optionsValue: 'index', value: selectedIndex">{
      scopedScript(
        "req(['observable', 'knockout'], function (O, ko) { ko.applyBindings({ options: O.makeObservableArray(optionsId), selectedIndex: O.makeObservable(selectedIndexId) }, this); });",
        Json.obj("optionsId" -> _optionsConnection.id, "selectedIndexId" -> _selected.id)
      )
    }</select>
}

object DropDown {
  private val defaultToString: Any => String = _.toString
}