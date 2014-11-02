package notebook.front.widgets

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import notebook._, JSBus._

import notebook.front._


class Sql(call: String) extends Widget {

  private[this] val sqlInputRegex = "(\\{[^\\}]+\\})".r
  private[this] val sqlTypedInputRegex = "^\\{([^:]+):(.*)\\}$".r

  val parts:List[(String, TypedInput[_])] = {
    val inputs = sqlInputRegex.findAllMatchIn(sql).toList
    val sqlGen = Sql(inputs match {
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
    })
  }

  val sqlp = parts.map(_._1).map(x => s"${qs}$x${qs}")
  val ws = parts.map(_._2.widget).mkString(" ++ ")

  lazy val toHtml = <input data-bind="value: value">{
    scopedScript(
      """require( ['observable', 'knockout'],
                  function (Observable, ko) {
                    ko.applyBindings({
                      value: Observable.makeObservable(valueId)
                    }, this);
                  }
                )""",
      ("valueId" -> connection.id)
    )
  }</input>
}

sealed trait TypedInput[T] {
  def name:String
  def apply(s:String):T
  def widget:String
}
case class BooleanInput(name:String) extends TypedInput[Boolean]{
  def apply(s:String):Boolean = s.toBoolean
  val widget = ???
}
case class CharInput(name:String) extends TypedInput[Char]{
  def apply(s:String):Char = s.head
  val widget = ???
}
case class StringInput(name:String) extends TypedInput[String]{
  def apply(s:String):String = s
  val widget = """(new InputBox(""))"""
}
case class DateInput(name:String) extends TypedInput[java.util.Date]{
  def apply(s:String):java.util.Date = new java.util.Date(s.toLong)
  val widget = ???
}
case class IntInput(name:String) extends TypedInput[Int]{
  def apply(s:String):Int = s.toInt
  val widget = ???
}
case class LongInput(name:String) extends TypedInput[Long]{
  def apply(s:String):Long = s.toLong
  val widget = ???
}
case class FloatInput(name:String) extends TypedInput[Float]{
  def apply(s:String):Float = s.toFloat
  val widget = ???
}
case class DoubleInput(name:String) extends TypedInput[Double]{
  def apply(s:String):Double = s.toDouble
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