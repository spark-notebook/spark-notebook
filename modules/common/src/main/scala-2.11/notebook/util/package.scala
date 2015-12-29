package notebook

import notebook.util.InterpreterUtil

package object util {
  implicit def toInterpreterUtil(i:scala.tools.nsc.interpreter.IMain):InterpreterUtil = new InterpreterUtil(i)
}