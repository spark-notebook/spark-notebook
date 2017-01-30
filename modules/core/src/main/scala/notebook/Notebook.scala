package notebook

import notebook.NBSerializer._

case class Notebook(
                     metadata: Option[Metadata] = None,
                     cells: Option[List[Cell]] = Some(Nil),
                     worksheets: Option[List[Worksheet]] = None,
                     autosaved: Option[List[Worksheet]] = None,
                     nbformat: Option[Int]
                   ) {
  def name = metadata.map(_.name).getOrElse("Anonymous")
  def normalizedName: String = {
    val n = name.toLowerCase.replaceAll("[^\\.a-z0-9_-]", "-")
    n.dropWhile(_ == '-').reverse.dropWhile(_ == '-').reverse
  }
  def updateMetadata(newMeta: Option[Metadata]): Notebook = this.copy(metadata = newMeta)
}

object Notebook {
  def getNewUUID: String = java.util.UUID.randomUUID.toString

  def deserialize(str: String): Option[Notebook] = NBSerializer.fromJson(str)

  def serialize(nb: Notebook): String = NBSerializer.toJson(nb)
}
