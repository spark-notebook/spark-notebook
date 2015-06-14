package notebook.front

import scala.runtime.BoxedUnit
import scala.xml.{NodeBuffer, NodeSeq, Text}
import org.apache.spark.sql.DataFrame

/**
 * Typeclass for rendering objects of a specific type. Implement one of these and import it
 * in your notebook to change how objects of the specified type get presented after evaluation.
 */
trait Renderer[-A] {
  def render(value: A): NodeSeq
}

class WidgetRenderer[-A](toWidget: A => Widget) extends Renderer[A] {
  def render(value: A) = toWidget(value).toHtml
}

object Renderer extends LowPriorityRenderers {

  implicit object htmlAsItself extends Renderer[NodeSeq] {
    def render(value: NodeSeq) = value
  }

  implicit object nodeBufferAsItself extends Renderer[NodeBuffer] {
    def render(value: NodeBuffer) = value
  }

  implicit object widgetAsItself extends Renderer[Widget] {
    def render(value: Widget) = value.toHtml
  }

  implicit object stringAsItself extends Renderer[String] {
    def render(value: String) = Text(value)
  }

  implicit object anyValAsItself extends Renderer[AnyVal] {
    def render(value: AnyVal) = {
      if (value == BoxedUnit.UNIT) {
        NodeSeq.Empty
      } else {
        Text(value.toString)
      }
    }
  }

}

trait LowPriorityRenderers {

  import widgets._

  implicit object mapAsTable extends Renderer[Map[_, _]] {
    def render(x: Map[_, _]) = if (x.isEmpty) {
      widgets.text("")
    } else {
      display(Left(x.toSeq))
    }
  }

  implicit object seqAsTable extends Renderer[Seq[_]] {
    def render(x: Seq[_]) = x match {
      case Nil => widgets.layout(0, Seq(widgets.text("")))
      case _ => display(Left(x))
    }
  }

  implicit object arrayAsTable extends Renderer[Array[_]] {
    def render(x: Array[_]) = x match {
      case x if x.isEmpty => widgets.layout(0, Seq(widgets.text("")))
      case _ => display(Left(x.toSeq))
    }
  }

  implicit object dataFrameAsTable extends Renderer[DataFrame] {
    def render(x: DataFrame) = x.take(1) match {
      case x if x.isEmpty => widgets.layout(0, Seq(widgets.text("")))
      case _ => display(Right(x))
    }
  }

}