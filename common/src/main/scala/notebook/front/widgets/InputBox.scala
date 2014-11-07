package notebook.front.widgets

import org.json4s.JsonDSL._
import org.json4s.JsonAST.{JString, JValue}

import notebook._, JSBus._

import notebook.front._


object types {
  sealed trait InputType[T] {
    def tpe:String
    val extra:Map[String, String] = Map.empty
  }
  implicit object BooleanType extends InputType[Boolean] {
    val tpe = "boolean" // TODO → extract a super type, put Input under, put CheckBox beside
  }
  implicit object CharType extends InputType[Char] {
    val tpe = "text" // TODO → extract a super type, put Input under, put CheckBox beside
    override val extra = Map("maxlength" -> "1")
  }
  implicit object TextType extends InputType[String] {
    val tpe = "text"
  }
  implicit object IntType extends InputType[Int] {
    val tpe = "number"
  }
  implicit object LongType extends InputType[Long] {
    val tpe = "number"
  }
  implicit object FloatType extends InputType[Float] {
    val tpe = "number"
    override val extra = Map("step" -> "0.01")
  }
  implicit object DoubleType extends InputType[Double] {
    val tpe = "number"
    override val extra = Map("step" -> "0.01")
  }
  implicit object DateType extends InputType[java.util.Date] {
    val tpe = "date"
  }
}

import types._

class InputBox[T](initial: T, label:String="")(implicit t:InputType[T], val codec:Codec[JValue, T])
  extends Widget with SingleConnector[T] {

  val id = "input-"+dataConnection.id
  lazy val toHtml = {
    val ll = <label for={id}>{label}</label>
    val in = <input id={id} type={t.tpe} name={id} data-bind="value: value">{
              scopedScript(
                """require( ['observable', 'knockout'],
                            function (Observable, ko) {
                              ko.applyBindings({
                                value: Observable.makeObservable(valueId)
                              }, this);
                            }
                          )""",
                ("valueId" -> dataConnection.id)
              )
            }</input>
    val nin = t.extra.map{ case (a,v) => new xml.UnprefixedAttribute(a, v, xml.Null) }.foldLeft(in)(_ % _)
    ll ++ nin
  }
}