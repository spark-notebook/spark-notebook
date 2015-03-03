package notebook.util

import play.api.libs.json.Json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import play.api.libs.json.JsBoolean
import play.api.libs.json.Json.JsValueWrapper

object Reflector{
   val ru = scala.reflect.runtime.universe
   val m = ru.runtimeMirror(getClass.getClassLoader)
   
   def objToTerms(obj: Any) = {
     val im = m.reflect(obj)
     (im.symbol.toType.members.toList.filter(a => a.isTerm && !a.isMethod).reverse, im)
   }
   
   def numOfFields(obj: Any) = {
     objToTerms(obj)._1.size
   }
   
   def toFieldNameArray(obj: Any) = {
	   val fields = objToTerms(obj)
	   fields._1.map( f => f.name.toString)
   }
   
   def toFieldValueArray(obj: Any) = {
     
	   val fields = objToTerms(obj)
	   fields._1.map( f => fields._2.reflectField(f.asTerm).get)
   }
   
   def toObjArray(obj: Any) = {
	   val fields = objToTerms(obj)
	   val fieldsJson = fields._1.map( f => f.name.toString -> fields._2.reflectField(f.asTerm).get)
	  
	   fieldsJson
   }
   
}