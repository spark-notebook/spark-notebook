/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook
package widgets

import com.bwater.notebook.Widget
import net.liftweb.json.JsonAST.{JValue, JArray, JInt}
import net.liftweb.json.JsonDSL._
import net.liftweb.json.DefaultFormats
import rx.Observable

class DropDown[A](options: Seq[A], toString: A=>String = (a:A)=>a.toString) extends Widget  {

  implicit val formats = DefaultFormats // JSON formats

  // Just implement the second function here to support option changing in JavaScript
  private lazy val _optionsConnection = JSBus.createConnection
  lazy val optionsConnection = _optionsConnection.biMap[Seq[A]](
    (s: Seq[A]) => JArray(s.zipWithIndex map { case (opt, i) => ("text" -> toString(opt)) ~ ("index" -> i) } toList), (_:JValue) => Seq[A]() )

  private lazy val _selected = JSBus.createConnection
  lazy val selected = _selected.biMap[A]((a:A) => JInt(options.indexOf(a)), (v:JValue) => options(v.extract[Int]))

  optionsConnection <-- Connection.just(options)

  lazy val toHtml =
    <select data-bind="options: options, optionsText: 'text', optionsValue: 'index', value: selectedIndex">{
      scopedScript(
        "require(['observable', 'knockout'], function (O, ko) { ko.applyBindings({ options: O.makeObservableArray(optionsId), selectedIndex: O.makeObservable(selectedIndexId) }, this); });",
        ("optionsId" -> _optionsConnection.id) ~ ("selectedIndexId" -> _selected.id)
      )
    }</select>
}

object DropDown {
  private val defaultToString: Any => String = _.toString
}