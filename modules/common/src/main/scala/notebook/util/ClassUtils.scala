package notebook.util

object ClassUtils {

  def getSimpleName(clazz: Class[_]): String = {
    def isAsciiDigit(c: Char) = '0' <= c && c <= '9'

    val enclosingClass = clazz.getEnclosingClass
    val name = clazz.getName
    if (enclosingClass == null) {
      name substring ((name lastIndexOf '.') + 1)
    } else {
      val simpleName = name substring enclosingClass.getName.length
      val length = simpleName.length
      if (length < 1 || (simpleName charAt 0) != '$') {
        simpleName
      } else {
        var index: Int = 1
        while (index < length && isAsciiDigit(simpleName charAt index)) {
          index += 1
        }
        simpleName substring index
      }
    }
  }

}