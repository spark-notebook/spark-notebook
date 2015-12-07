package notebook.util

object StringUtils {
  import scala.util.matching.Regex
  val varEnv = new Regex("""\$\{([_a-zA-Z0-9:]+)\}+""", "var")
  def updateWithVarEnv(s:String) = {
    val x = varEnv.replaceAllIn(s, m => sys.env(m.group("var")))
    x
  }
}