package notebook.front.widgets

import scala.util._
import scala.concurrent._

import akka.actor._

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import org.apache.spark.sql.{SQLContext, SchemaRDD}

import notebook._, JSBus._
import JsonCodec._

import notebook.front._

class Sql(sqlContext:SQLContext, call: String) extends Widget with notebook.util.Logging {

  private[this] val sqlInputRegex = "(\\{[^\\}]+\\})".r
  private[this] val sqlTypedInputRegex = "^\\{([^:]+):(.*)\\}$".r

  private type Item = (String, TypedInput[_])

  val (parts:List[Item], after:String) = {
    val inputs = sqlInputRegex.findAllMatchIn(call).toList
    val r = inputs match {
      case Nil => Nil
      case x :: Nil =>
        val b = x.before.toString
        val sqlTypedInputRegex(tpe, name) = x.matched
        val r = (b, TypedInput(tpe, name.trim))
        r :: List.empty[Item]
      case x :: xs =>
        val b = x.before.toString
        val sqlTypedInputRegex(tpe, name) = x.matched
        val h  = (b, TypedInput(tpe, name.trim))
        val t = inputs.sliding(2).toList.map{
                  case i::j::Nil =>
                    val b = j.before.toString.substring(i.before.toString.size+i.matched.size)
                    val sqlTypedInputRegex(tpe, name) = j.matched
                    (b, TypedInput(tpe, name.trim))
                }
        h :: t
    }
    (r, Try(inputs.last.after.toString).toOption.getOrElse(""))
  }

  import rx.lang.scala.{Observable => RxObservable, Observer => RxObserver, _}

  val mergedObservables:RxObservable[(String, Any)] = {
    val l:List[RxObservable[(String, Any)]] = parts.map{ p =>
      val ob = p._2.widget.currentData.observable.inner//.doOnEach(x => logDebug("########:"+x.toString))
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

  val subject:Subject[Option[Try[SchemaRDD]]] = subjects.ReplaySubject(1)

  var result:Subject[Any] = subjects.ReplaySubject(1)

  def updateValue(c:String) = {
    val tried:Option[Try[SchemaRDD]] = Some(Try{sqlContext.sql(c)})
    logInfo(" Tried => " + tried.toString)
    subject.onNext(tried)
    sql(tried)
    tried
  }

  sql {
    parts match {
      case Nil => updateValue(call)
      case xs => None
    }
  }

  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  def react[A](f:SchemaRDD => A, w:SingleConnectedWidget[A]) = {
    result.subscribe(x => w(x.asInstanceOf[A])) //argl → asInstanceOf
    val sub = (o:Option[Try[SchemaRDD]]) => {
      o match {
        case Some(Success(s)) =>
          val r = f(s)
          result.onNext(r)
        case x =>
          logError("ARRrrggllll → " + x.toString)
      }
    }
    subject.subscribe(sub)
    //subject.orElse(None).subscribe(sub)
    w
  }

  mergedObservables.subscribe(new RxObserver[(String, Any)]() {
    val values:collection.mutable.Map[String, Any] = collection.mutable.HashMap[String, Any]().withDefaultValue("")
    override def onNext(value: (String, Any)): Unit = {
      values += value
      val s = parts.map { case (before, input) =>
        val vv = values(input.name)
        before + vv.toString
      }.mkString("")
      val c  = s + after
      updateValue(c)
    }
  })

  val ws:Widget = {
    val ps = parts.map(_._2.widget) match {
      case Nil => out
      case xs => xs.reduce((x:Widget, y:Widget) => x ++ y)
    }
    ps ++ sql
  }

  lazy val toHtml = ws.toHtml
}

object Sql {
  implicit def toWidget(sql:Sql):Widget = sql.ws
}

import notebook.front.widgets.types._

sealed trait TypedInput[T] {
  def name:String
  def widget:Widget with SingleConnector[T]
}
case class BooleanInput(name:String) extends TypedInput[Boolean]{
  val widget = new InputBox[Boolean](false, name)
}
case class CharInput(name:String) extends TypedInput[Char]{
  val widget = new InputBox[Char](' ', name)
}
case class StringInput(name:String) extends TypedInput[String]{
  val widget = new InputBox[String]("", name)
}
case class DateInput(name:String) extends TypedInput[java.util.Date] {
  implicit val d:java.text.DateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd")
  val widget = new InputBox[java.util.Date](new java.util.Date(), name)
}
case class IntInput(name:String) extends TypedInput[Int]{
  val widget = new InputBox[Int](0, name)
}
case class LongInput(name:String) extends TypedInput[Long]{
  val widget = new InputBox[Long](0, name)
}
case class FloatInput(name:String) extends TypedInput[Float]{
  val widget = new InputBox[Float](0, name)
}
case class DoubleInput(name:String) extends TypedInput[Double]{
  val widget = new InputBox[Double](0, name)
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