package notebook.front.widgets

import scala.util._

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import org.apache.spark.sql.{SQLContext, SchemaRDD}

import notebook._, JSBus._

import notebook.front._


class Sql(sqlContext:SQLContext, call: String) extends Widget with notebook.util.Logging {

  private[this] val sqlInputRegex = "(\\{[^\\}]+\\})".r
  private[this] val sqlTypedInputRegex = "^\\{([^:]+):(.*)\\}$".r

  val parts:List[(String, TypedInput[_])] = {
    val inputs = sqlInputRegex.findAllMatchIn(call).toList
    inputs match {
      case Nil => Nil
      case x :: Nil =>
        val b = x.before.toString
        val sqlTypedInputRegex(tpe, name) = x.matched
        (b, TypedInput(tpe, name)) :: Nil
      case x :: xs =>
        val b = x.before.toString
        val sqlTypedInputRegex(tpe, name) = x.matched
        val h  = (b, TypedInput(tpe, name))
        val t = inputs.sliding(2).toList.map{
                  case i::j::Nil =>
                    val b = j.before.toString.substring(i.before.toString.size+i.matched.size)
                    val sqlTypedInputRegex(tpe, name) = j.matched
                    (b, TypedInput(tpe, name))
                }
        h :: t
    }
  }

  import rx.lang.scala.{Observable => RxObservable, Observer => RxObserver, _}

  val mergedObservables:RxObservable[(String, Any)] = {
    val l:List[RxObservable[(String, Any)]] = parts.map{ p =>
      val ob = p._2.widget.currentData.observable.inner//.doOnEach(x => logInfo(x.toString))
      val o:RxObservable[(String, Any)] = ob.map((d:Any) => (p._2.name, d))
      o
    }
    RxObservable.from(l).flatten
  }

  val sql = new SingleConnector[Option[Try[SchemaRDD]]] with Widget {
    implicit val codec = new Codec[JValue, Option[Try[SchemaRDD]]] {
      def encode(x:JValue):Option[Try[SchemaRDD]] = None
      def decode(x:Option[Try[SchemaRDD]]):JValue = JString {
        x.flatMap(t => t match {
          case Success(s) => Some(s.toString)
          case Failure(ex) => Some(ex.getMessage)
        }).getOrElse("<no enough info>")
      }
    }

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
  sql(None)

  mergedObservables.subscribe(new RxObserver[(String, Any)]() {
    val values:collection.mutable.Map[String, Any] = collection.mutable.HashMap[String, Any]().withDefaultValue("")
    override def onNext(value: (String, Any)): Unit = {
      values += value
      val s = parts.map { case (before, input) =>
        val vv = values(input.name)
        before + vv.toString
      }.mkString(" ")
      val tried:Option[Try[SchemaRDD]] = Some(Try{sqlContext.sql(s)})
      logDebug(tried.toString)
      sql(tried)
    }
  })

  val ws:Widget = {
    val ps = parts.map(_._2.widget)
                  .reduce((x:Widget, y:Widget) => x ++ y)
    ps ++ sql
  }

  lazy val toHtml = ws.toHtml
}

object Sql {
  implicit def toWidget(sql:Sql):Widget = sql.ws
}

sealed trait TypedInput[T] {
  def name:String
  def widget:Widget with SingleConnector[T]
}
case class BooleanInput(name:String) extends TypedInput[Boolean]{
  val widget = ???
}
case class CharInput(name:String) extends TypedInput[Char]{
  val widget = ???
}
case class StringInput(name:String) extends TypedInput[String]{
  val widget = new InputBox("", name)
}
case class DateInput(name:String) extends TypedInput[java.util.Date]{
  val widget = ???
}
case class IntInput(name:String) extends TypedInput[Int]{
  val widget = ???
}
case class LongInput(name:String) extends TypedInput[Long]{
  val widget = ???
}
case class FloatInput(name:String) extends TypedInput[Float]{
  val widget = ???
}
case class DoubleInput(name:String) extends TypedInput[Double]{
  val widget = ???
}
object TypedInput {
  def apply(tpe:String, name:String):TypedInput[_] = tpe match {
    case "Boolean" => BooleanInput(name)
    case "Char"    => CharInput(name)
    case "String"  => StringInput(name)
    case "Date"    => DateInput(name)
    case "Int"     => IntInput(name)
    case "Long"    => LongInput(name)
    case "Float"   => FloatInput(name)
    case "Double"  => DoubleInput(name)
  }
}