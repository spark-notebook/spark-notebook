package notebook.front

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import org.json4s.native._
import xml.{NodeSeq, UnprefixedAttribute, Null}

import org.json4s.DefaultFormats

import notebook._, front._, widgets._
import notebook._, JSBus._

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets {
  def scopedScript(content: String, data: JObject = null) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", compactJson(renderJValue(data)), Null)
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
        """ require(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId),
                    style: O.makeObservable(styleId)
                  },
                  this
                );
              }
            );
        """,
        ("valueId" -> _currentValue.id) ~ ("styleId" -> _currentStyle.id)
      )}</p>)
  }

  def out = new SingleConnectedWidget[String] {
    implicit val codec = JsonCodec.strings

    lazy val toHtml = <p data-bind="text: value">{
      scopedScript(
        """ require(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId)
                  },
                  this
                );
              }
            );
        """,
        ("valueId" -> dataConnection.id)
      )}</p>
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