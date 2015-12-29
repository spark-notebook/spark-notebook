package notebook.util

class InterpreterUtil(i:org.apache.spark.repl.SparkIMain) {
  def apply(s:String) = i.interpret(s)
}