package notebook.kernel.repl.common

import java.io.ByteArrayOutputStream

import scala.concurrent.{Future, Promise}

import notebook.kernel.EvaluationResult
import notebook.util.Match

case class NameDefinition(name: String, tpe: String, references: List[String]) {
  def definesType: Boolean = {
    tpe == NameDefinition.TYPE_DEFINITION
  }
}
object NameDefinition {
  val TYPE_DEFINITION = "type"
}

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

  def endInitCommand: List[(String, String)] = List(("end", "\"END INIT\""))

  def addCp(newJars:List[String]): (ReplT, () => Unit)
  def classServerUri: Option[String]
  def complete(line: String, cursorPosition: Int): (String, Seq[Match])
  def evaluate(code: String,
               onPrintln: String => Unit = _ => (),
               onNameDefinion: NameDefinition => Unit  = _ => ()
              ): (EvaluationResult, String)
  def getTypeNameOfTerm(termName: String): Option[String]
  def setInitFinished(): Unit
  def objectInfo(line: String, position:Int): Seq[String]
  def sparkContextAvailable: Boolean
  def stop(): Unit

}

object ReplT {
  def create(opts:List[String], deps:List[String]):ReplT = {
    val replClass = getClass.getClassLoader.loadClass("notebook.kernel.Repl")
    replClass.getConstructor(classOf[List[String]], classOf[List[String]])
              .newInstance(opts, deps)
              .asInstanceOf[ReplT]
  }
}

object ReplHelpers {
  final def formatShortDuration(durationMillis: Long): String = {
    import org.joda.time._
    import org.joda.time.format._
    val duration = new Duration(durationMillis)
    val formatter = new PeriodFormatterBuilder()
      .appendHours
      .appendSuffix("h")
      .appendMinutes
      .appendSuffix("m")
      .appendSecondsWithOptionalMillis
      .appendSuffix("s")
      .toFormatter;
    formatter.print(duration.toPeriod());
  }
}