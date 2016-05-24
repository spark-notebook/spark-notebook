package notebook.front.widgets

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._

trait Lists extends Generic with Utils {
  def ul(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None) = new HtmlList(capacity, initData, prefill)
  def ol(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None) = new HtmlList(capacity, initData, prefill, false)

  class HtmlList(capacity:Int=10, initData:Seq[String]=Nil, prefill:Option[String]=None, unordered:Boolean=true) extends DataConnectedWidget[String] {
    implicit val singleCodec:Codec[JsValue, String] = JsonCodec.strings

    var data = (initData.size, prefill) match {
      case (0, None) => Seq.empty[String]
      case (x, None) => initData
      case (0, Some(i)) => Seq.fill(capacity)(i)
      case (x, Some(i)) => initData.padTo(capacity, i)
    }

    apply(data)

    lazy val reactivity = scopedScript(
                            """
                                |req(
                                |['observable', 'knockout'],
                                |function (O, ko) {
                                |  ko.applyBindings({
                                |      value: O.makeObservable(valueId)
                                |    },
                                |    this
                                |  );
                                |});
                            """stripMargin,
                            Json.obj("valueId" -> dataConnection.id)
                          )


    lazy val toHtml = unordered match {
      case true => <ul data-bind="foreach: value"><li data-bind="html: $data"></li>{reactivity}</ul>
      case false => <ol data-bind="foreach: value"><li data-bind="html: $data"></li>{reactivity}</ol>
    }


    override def apply(d:Seq[String]) {
      data = if (d.size > capacity) {
         d.drop(d.size - capacity)
      } else {
        d
      }
      super.apply(data)
    }

    def append(s:String) {
      apply(data :+ s)
    }

    def appendAll(s:Seq[String]) {
      apply(data ++ s)
    }
  }
}