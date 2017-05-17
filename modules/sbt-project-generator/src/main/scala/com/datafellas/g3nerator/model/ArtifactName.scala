package com.datafellas.g3nerator.model


trait ArtifactName {
  def name: String
}

object ArtifactName {
  val NonAlphanumericToDashesTrimmed: String => String = {
    _.toLowerCase.replaceAll("[^\\.a-z0-9-]+", "-").dropWhile(c => !c.isLetter).reverse.dropWhile(c => !c.isLetter).reverse
  }
  val AlphanumericCamelCase: String => String = _.toLowerCase.split("[\\W_]+").map(_.capitalize).mkString("")
  val AlphanumericLowerCase: String => String = _.toLowerCase.replaceAll("[\\W_]+", "")

}

case class DebianName(source: String) extends ArtifactName {
  val name: String = ArtifactName.NonAlphanumericToDashesTrimmed(source)
}

case class ClassName(source: String) extends ArtifactName {
  val name: String = ArtifactName.AlphanumericCamelCase(source)
}

case class PackageName(source: String) extends ArtifactName {
  def name: String = ArtifactName.AlphanumericLowerCase(source)
}

case class LibraryName(source: String) extends ArtifactName {
  def name: String  = ArtifactName.NonAlphanumericToDashesTrimmed(source)
}
