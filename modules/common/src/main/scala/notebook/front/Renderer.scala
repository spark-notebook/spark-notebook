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
      
      val xSeq = x.toSeq.map(e => MapPoint(e._1, e._2))
      
      reflectAndBuildUI(xSeq)
      
    }
  }
  
  case class ChartPoint(X: Any, Y: Any)
  case class MapPoint(Key: Any, Value: Any)
  
  private def reflectAndBuildUI(data: Seq[_])= {
	  	val firstElem = data.head
		val numOfFields = if(firstElem.isInstanceOf[String]) 1 else Reflector.numOfFields(firstElem)
		val members = Reflector.toFieldValueArray(firstElem)
	    
		val headers: Seq[String] = if(numOfFields > 1) Reflector.toFieldNameArray(data.head) else Nil
	    
	    def normalize(obj: Any): Seq[Any] = if(numOfFields > 1) Reflector.toFieldValueArray(obj) else Seq(obj)
	  
	    val values: Seq[String] =( if (data.lengthCompare(25) < 0) ((data flatMap normalize ).map(_.toString)) else ((data.take(24) flatMap normalize).map(_.toString) :+ "...")) 
	    
	    
	    val table = widgets.table(numOfFields, values.map(v => widgets.text(v)), headers.map(v => widgets.text(v)))
	    if(numOfFields == 2 && isNumber(members(1)) ){
	      
	    	def toJson(obj: Any) = Reflector.toObjArray(obj)
	    	val jsons = if (data.lengthCompare(25) < 0) (data flatMap toJson) else (data.take(24) flatMap toJson)
	    	val tabs = ("bar-chart", widgets.barChart(numOfFields, jsons)) :: ("pie-chart", widgets.pieChart(numOfFields, jsons)) :: ("table",table) :: Nil
	    	widgets.tabControl(tabs.reverse)
	    }else{
	    	widgets.html(table)
	    }
  }
  
  implicit object seqAsTable extends Renderer[Seq[_]] {
    def render(x: Seq[_]) = {
      
      x match {
        case Nil => widgets.layout(0, Seq(widgets.text("")))
        case _ => {
        			var firstElem = x.head
        			var numOfFields = if(firstElem.isInstanceOf[String]) 1 else Reflector.numOfFields(firstElem)
			        
			        val data = 
			        	(numOfFields, firstElem) match {
			        	  case (1, o @ (_:Int | _:Float | _:Double )) => x.zipWithIndex.map(e => ChartPoint(e._2, e._1))
			        	  case (2, (a,b)) => x.map(e =>	e match {
			        		  							  case (xx,yy) => ChartPoint(xx, yy)
			        	  								})
			        	  case _ => x
			        	}
        			
        			reflectAndBuildUI(data)
			        
        	}
      }	
      
      
      
    }
  }
  
  def isNumber(obj: Any) = obj.isInstanceOf[Int] || obj.isInstanceOf[Float] || obj.isInstanceOf[Double]
}







