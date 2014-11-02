package notebook.front.widgets

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import org.apache.spark.sql.SQLContext

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

  val sqlp = parts.map(_._1)
  val ws:Widget = parts.map(_._2.widget).reduce((x:Widget, y:Widget) => x ++ y)

  import rx.lang.scala.{Observable => RxObservable, Observer => RxObserver, _}

  val mergedObservables:RxObservable[Seq[(String, Any)]] = {
    val l:List[RxObservable[(String, Any)]] = parts.map{ p =>
      val ob = p._2.widget.currentData.observable.inner
      ob.subscribe(x => logInfo(x.toString))
      val o:RxObservable[(String, Any)] = ob.map((d:Any) => (p._2.name, d))
      o
    }
    // just a try in case Observable.from(l) wouldn't stop → hence zip won't start, because it needs to know N
    l.head.zip(l.tail.head).map(v => v._1 :: v._2 :: Nil)
    //RxObservable.zip(RxObservable.from(l))
  }

  var sql:Connection[String] = Connection.just("")

  mergedObservables.subscribe(
    onNext = (v:Seq[(String, Any)]) => {
          logInfo("============ssssss============")
          val values = v.toMap
          val s = parts.map { case (before, input) =>
            val vv = values(input.name)
            before + vv.toString
          }.mkString(" ")
          logInfo(s)
          sql <-- Connection.just(s)
        },
    onError = (t:Throwable) => logError("============ssssss============\n"+"Ouch in merged", t),
    onCompleted = () => logWarn("============ssssss============\n"+" Merged completed ! ")
  )

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