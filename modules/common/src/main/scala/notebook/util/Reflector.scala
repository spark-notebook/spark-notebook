package notebook.util

object Reflector {
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
    fields._1.map(f => f.name.toString.trim)
  }

  def toFieldValueArray(obj: Any) = {
    val fields = objToTerms(obj)
    fields._1.map(f => fields._2.reflectField(f.asTerm).get)
  }

  def toObjArray(obj: Any) = {
    val fields = objToTerms(obj)
    fields._1.map(f => f.name.toString.trim -> fields._2.reflectField(f.asTerm).get)
  }
}