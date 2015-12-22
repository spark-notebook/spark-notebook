package notebook.front.widgets

import notebook._
import notebook.front._
import play.api.libs.json._


object types {

  sealed trait InputType[T] {
    def tpe: String

    val extra: Map[String, String] = Map.empty
  }

  implicit object BooleanType extends InputType[Boolean] {
    val tpe = "boolean" // TODO → extract a super type, put Input under, put CheckBox beside
  }

  implicit object CharType extends InputType[Char] {
    val tpe = "text"
    // TODO → extract a super type, put Input under, put CheckBox beside
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

object extraTypes {
  implicit def SliderType[T](min:T, max:T, step:T)(implicit num: Numeric[T]) = new types.InputType[T] {
    val tpe = "range"
    override val extra = Map("min" → min.toString, "max" → max.toString, "step" → step.toString)
  }
}

import notebook.front.widgets.types._

class InputBox[T](initial: T, label: String = "")
    (implicit t: InputType[T], val codec: Codec[JsValue, T])
  extends Widget with SingleConnector[T] {

  val id = "input-" + dataConnection.id
  lazy val toHtml = {
    val ll = <label for={id}>
      {label}
    </label>
    val in = <input id={id} type={t.tpe} name={id} data-bind="textInput: value, fireChange: true, valueUpdate: 'input'">
      {scopedScript(
        """|req(
           | ['observable', 'knockout'],
           | function (Observable, ko) {
           |   var obs = Observable.makeObservable(valueId)
           |                       .extend({ rateLimit: { //throttle
           |                                   timeout: 500,
           |                                   method: "notifyWhenChangesStop"
           |                                 }
           |                               }
           |                       );
           |   ko.applyBindings({
           |     value: obs
           |   }, this);
           |   obs(valueInit);
           | }
           |)""".stripMargin,
        Json.obj("valueId" -> dataConnection.id, "valueInit" → codec.decode(initial)),
        Some("#" + id)
      )}
    </input>
    val nin = t.extra.map { case (a, v) => new xml.UnprefixedAttribute(a, v, xml.Null) }.foldLeft(in)(_ % _)
    ll ++ nin
  }
}