package notebook.util

class InterpreterUtil(i:scala.tools.nsc.interpreter.IMain) {
  def apply(s:String) = i.interpret(s, true)
}