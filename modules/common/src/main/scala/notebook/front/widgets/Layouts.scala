package notebook.front.widgets

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.magic._

trait Layouts extends Generic with Utils {

  def layout(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil): Widget = html(table(width, contents, headers ))

  def table(width: Int, contents: Seq[Widget], headers: Seq[Widget] = Nil):Widget =
    <div class="table-container table-responsive">
    <table class="table">
      <thead>{
        (headers) grouped width map { row =>
          <tr>{
            row map { html => <th>{html}</th> }
          }</tr>
        }
    }
      </thead>
      <tbody>{
          (contents) grouped width map { row =>
          <tr>{
            row map { html => <td>{html}</td> }
          }</tr>
        }
      }
      </tbody>
    </table></div>

  def row(contents: Widget*)    = layout(contents.length, contents)
  def column(contents: Widget*) = layout(1, contents)

  def multi(widgets: Widget*) = html(NodeSeq.fromSeq(widgets.map(_.toHtml).flatten))

  def containerFluid(conf:List[List[(Widget,Int)]]):Widget = html(
    <div class="container-fluid">{
      conf.map { rows =>
        <div class="row">{
          rows.map { case (w, i) =>
            val cl = "col-md-"+i
            <div class={cl}>{w}</div>
          }
        }</div>
      }
    }</div>
  )
}