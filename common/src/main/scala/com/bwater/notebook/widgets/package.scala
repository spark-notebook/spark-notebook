/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import net.liftweb.json.JsonAST.{JString, JValue, JObject, render}
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Printer.compact
import xml.{NodeSeq, UnprefixedAttribute, Null}
import net.liftweb.json.DefaultFormats

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets {
  def scopedScript(content: String, data: JObject = null) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", compact(render(data)), Null)
  }


  def text(value: String) = html(xml.Text(value))

  def text(value: Connection[String], style: Connection[String] = Connection.just("")) = {
    val _currentValue = JSBus.createConnection
    val currentValue = _currentValue biMap JsonCodec.strings
    currentValue <-- value

    val _currentStyle = JSBus.createConnection
    val currentStyle = _currentStyle biMap JsonCodec.strings
    currentStyle <-- style

    html(<p data-bind="text: value, style: style">{
      scopedScript(
        "require(['observable', 'knockout'], function (O, ko) { ko.applyBindings({ value: O.makeObservable(valueId), style: O.makeObservable(styleId) }, this); });",
        ("valueId" -> _currentValue.id) ~ ("styleId" -> _currentStyle.id)
      )}</p>)
  }

  def html(html: NodeSeq): Widget = new SimpleWidget(html)

  def layout(width: Int, contents: Seq[Widget]): Widget = html(
    <table>{
      contents grouped width map { row =>
        <tr>{
          row map { html => <td>{html}</td> }
        }</tr>
      }
    }</table>)

  def row(contents: Widget*) = layout(contents.length, contents)
  def column(contents: Widget*) = layout(1, contents)

  def multi(widgets: Widget*) = html(NodeSeq.fromSeq(widgets.map(_.toHtml).flatten))
}