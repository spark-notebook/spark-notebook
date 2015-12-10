package notebook.front
package widgets

import scala.util.Random
import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import notebook._
import notebook.JsonCodec._

case class Flow() extends JsWorld[String, String] {
  import notebook.JSBus._

  implicit val singleToO = identity[String] _

  implicit val singleCodec = strings

  override val data:Seq[String] = Nil

  override val scripts = List(
    Script("flow", Json.obj())
  )

  override def apply(newData: Seq[String]) {
  }

  override def content = Some {
    <div>
      <div class="control">
        <button type="button" class="btn btn-xs">
          <span><i class="fa fa-square"/></span>
        </button>
        <button type="button" class="btn btn-xs">
          <span><i class="fa fa-arrow-right"/></span>
        </button>
      </div>
      <div class="jointgraph"></div>
      <div class="configuration"></div>
    </div>
  }
}