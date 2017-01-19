package notebook.util

object Reflector {
  val LOG = org.slf4j.LoggerFactory.getLogger(Reflector.getClass);

  val ru = scala.reflect.runtime.universe
  val m = ru.runtimeMirror(getClass.getClassLoader)

  def objToTerms(obj: Any): Option[(List[ru.Symbol], ru.InstanceMirror)] = try {
    val im = m.reflect(obj)
    val terms = im.symbol.toType.members.toList.filter(a => a.isTerm && !a.isMethod).reverse
    Some((terms, im))
  } catch {
    case x: Exception =>
      LOG.warn("Exception in objToTerms: "+x.getMessage)
      None
  }

  def numOfFields(obj: Any): Int = {
    toFieldNameArray(obj).size
  }

  def toFieldNameArray(obj: Any): List[String] = {
    toObjArray(obj).map(_._1)
  }

  def toFieldValueArray(obj: Any): List[Any] = {
    toObjArray(obj).map(_._2)
  }

  val tupleWeirdNames = "^(_[0-9]+)\\$mc.\\$sp$".r
  def extractName(s: ru.Symbol) = {
    s.name.toString.trim match {
      case tupleWeirdNames(name) => name
      case x => x
    }
  }

  def getFieldValue(field: ru.Symbol, reflector: ru.InstanceMirror): Option[(String, Any)] = {
    try {
      val fieldValue = reflector.reflectField(field.asTerm).get
      Some(extractName(field) -> fieldValue)
    } catch {
      case x: Exception =>
        LOG.warn(s"Cannot reflect field ${field.name}")
        None
    }
  }

  // Return a List of (fieldName -> fieldValue) excluding fields those reflection is failing
  def toObjArray(obj: Any): List[(String, Any)] = try {
    objToTerms(obj).map { case (terms, reflector) =>
      terms.flatMap(getFieldValue(_, reflector))
    }.getOrElse(Nil)
  } catch {
    case x: Exception =>
      LOG.warn("Exception in toObjArray: " + x.getMessage)
      Nil
  }
}
