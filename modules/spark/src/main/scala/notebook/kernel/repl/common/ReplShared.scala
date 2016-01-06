package notebook.kernel.repl.common

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
