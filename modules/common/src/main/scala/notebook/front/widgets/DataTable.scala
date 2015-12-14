package notebook.front.widgets

import io.continuum.bokeh._
import io.continuum.bokeh.widgets.{HandsonTable, TableColumn}
import notebook._
import notebook.front.third._
import play.api.libs.json._

class DataTable[T](data: Seq[T])(implicit val codec: Codec[JsValue, T]) {

  lazy val array: JsArray = JsonCodec.tSeq(codec).decode(data).asInstanceOf[JsArray]

  lazy val table = {
    val source = new ColumnDataSource()
    val keys = array(0).asInstanceOf[JsObject].keys.toList
    val columns = keys map { k =>
      (array(0) \ k).get match {
        case JsString(_) =>
          val values = array.value.map { i =>
            (i \ k).as[String]
          }
          source.addColumn(Symbol(k), values)
          val c = new TableColumn().field(k).header(k)
          c

        case JsNumber(n) =>
          val values = array.value.map { i =>
            (i \ k).as[Double]
          }
          source.addColumn(Symbol(k), values)
          val c = new TableColumn().field(k).header(k)
          c

        case JsBoolean(b) =>
          val values = array.value.map { i =>
            (i \ k).as[Boolean]
          }
          source.addColumn(Symbol(k), values)
          val c = new TableColumn().field(k).header(k)
          c
      }
    }
    new HandsonTable().source(source).columns(columns) //.editable(true
  }


  lazy val plot = Bokeh.widget(List(table))
}