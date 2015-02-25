package notebook.front

import xml.{NodeBuffer, Text, NodeSeq}
import runtime.BoxedUnit
import notebook.util._
import play.api.libs.json.JsObject
import play.api.libs.json.Json.JsValueWrapper

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
      if (value == BoxedUnit.UNIT) 
        NodeSeq.Empty 
      else{
        Text(value.toString)
      }
    }
  }
}

trait LowPriorityRenderers {
 
  implicit object mapAsTable extends Renderer[Map[_ , _]] {
    def render(x: Map[_ , _]) = {
      
      val numOfFields = 2
      val headers: Seq[String] = List("Key", "Value")
      
      def normalize(obj: Any): Seq[String] = Reflector.toFieldValueArray(obj).map(_.toString)
      
      val xSeq = x.toSeq
      val values: Seq[String] = if (xSeq.lengthCompare(25) < 0) (xSeq flatMap normalize) else (xSeq.take(24) flatMap normalize) :+ "..."
      
      widgets.layout(numOfFields, values.map(v => widgets.text(v)), headers.map(v => widgets.text(v)))
    }
  }
  
  implicit object seqAsTable extends Renderer[Seq[_]] {
    def render(x: Seq[_]) = {
      
      if(x.isEmpty){
        widgets.layout(0, Seq(widgets.text("")))
        
      }else{
        val numOfFields = Reflector.numOfFields(x.head)
        val headers: Seq[String] = if(numOfFields > 1) Reflector.toFieldNameArray(x.head) else Nil
      
        def numOfCols = if(numOfFields > 1) numOfFields else 1
        def normalize(obj: Any): Seq[Any] = if(numOfFields > 1) Reflector.toFieldValueArray(obj) else Seq(obj)
      
        val values: Seq[String] =( if (x.lengthCompare(25) < 0) ((x flatMap normalize ).map(_.toString)) else ((x.take(24) flatMap normalize).map(_.toString) :+ "...")) 
        
        val members = Reflector.toFieldValueArray(x.head)
        
        var tabs: List[(String, scala.xml.Elem)] = Nil
        if(numOfFields == 2 && isNumber(members(1)) ){
          
        	def toJson(obj: Any) = Reflector.toObjArray(obj)
        	val jsons = if (x.lengthCompare(25) < 0) (x flatMap toJson) else (x.take(24) flatMap toJson)
        	
        	tabs = tabs :+ ("bar", widgets.barChart(numOfCols, jsons))
        	tabs = tabs :+ ("pie", widgets.pieChart(numOfCols, jsons))
        }
        tabs = tabs :+ ("table", widgets.table(numOfCols, values.map(v => widgets.text(v)), headers.map(v => widgets.text(v))))
        widgets.tabControl(tabs)
      }
      
    }
  }
  
  def isNumber(obj: Any) = obj.isInstanceOf[Int] || obj.isInstanceOf[Float] || obj.isInstanceOf[Double]
}







