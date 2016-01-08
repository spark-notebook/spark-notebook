package notebook.kernel.repl.common

import java.io.ByteArrayOutputStream

import notebook.kernel.EvaluationResult
import notebook.util.Match

abstract class NameDefinition {
  def name: String
  def tpe: String
  def references: List[String]
}
object NameDefinition {
  def unapply(nd: NameDefinition) = Some(nd.name, nd.tpe, nd.references)
}
case class TypeDefinition(name: String, tpe: String, references: List[String]) extends NameDefinition
case class TermDefinition(name: String, tpe: String, references: List[String]) extends NameDefinition

class ReplOutputStream extends ByteArrayOutputStream {
  var aop: String => Unit = x => ()

  override def write(i: Int): Unit = {
    // CY: Not used...
    //      orig.value ! StreamResponse(i.toString, "stdout")
    super.write(i)
  }

  override def write(bytes: Array[Byte]): Unit = {
    // CY: Not used...
    //      orig.value ! StreamResponse(bytes.toString, "stdout")
    super.write(bytes)
  }

  override def write(bytes: Array[Byte], off: Int, length: Int): Unit = {
    val data = new String(bytes, off, length)
    aop(data)
    //      orig.value ! StreamResponse(data, "stdout")
    super.write(bytes, off, length)
  }
}

trait ReplT {

  def addCp(newJars:List[String]): (ReplT, () => Unit)

  def complete(line: String, cursorPosition: Int): (String, Seq[Match])

  def evaluate(code: String,
               onPrintln: String => Unit = _ => (),
               onNameDefinion: NameDefinition => Unit  = _ => ()
              ): (EvaluationResult, String)

  def getTypeNameOfTerm(termName: String): Option[String]

  def objectInfo(line: String, position:Int): Seq[String]

  def stop(): Unit
  //def restart(): Unit
  //def start: ReplT

}