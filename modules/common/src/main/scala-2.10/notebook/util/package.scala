package notebook

import notebook.util.InterpreterUtil

package object util {
  implicit def toInterpreterUtil(i:org.apache.spark.repl.SparkIMain):InterpreterUtil = new InterpreterUtil(i)
}