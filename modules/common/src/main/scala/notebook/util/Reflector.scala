package notebook.util

object Reflector {
  val LOG = org.slf4j.LoggerFactory.getLogger(Reflector.getClass);

  val ru = scala.reflect.runtime.universe
  val m = ru.runtimeMirror(getClass.getClassLoader)

  def objToTerms(obj: Any) = {
    val im = m.reflect(obj)
    (im.symbol.toType.members.toList.filter(a => a.isTerm && !a.isMethod).reverse, im)
  }

  def numOfFields(obj: Any) = {
    toFieldNameArray(obj).size
  }

  def toFieldNameArray(obj: Any) = {
    toObjArray(obj).map(_._1)
  }

  def toFieldValueArray(obj: Any) = {
    toObjArray(obj).map(_._2)
  }

  def toObjArray(obj: Any) = {
    val fields = objToTerms(obj)
    fields._1.map { f =>
      try{
        Some(f.name.toString.trim -> fields._2.reflectField(f.asTerm).get)
      } catch {
        case x =>
          LOG.warn(s"Cannot reflect field ${f.name}")
          None
      }
    }.collect{
      case Some(x) => x
    }
  }
}