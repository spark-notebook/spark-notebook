package notebook.kernel.repl.common

import org.apache.spark.repl.SparkIMain

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

object SparkInterpUtils {
  implicit class SparkInterpreterExtensions(interp: SparkIMain) {
    def getTypeNameOfTerm(termName: String): Option[String] = {
      val tpe = try {
        interp.typeOfTerm(termName).toString
      } catch {
        case exc: RuntimeException => println("Unable to get symbol type", exc); "<notype>"
      }
      tpe match {
        case "<notype>" => // "<notype>" can be also returned by typeOfTerm
          interp.classOfTerm(termName).map(_.getName)
        case _ =>
          // remove some crap
          Some(tpe.replace("iwC$", ""))
      }
    }
  }
}
