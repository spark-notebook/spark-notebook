package notebook

sealed trait Resource {
  def name:String
  def path:String
}

case class GenericFile(name:String, path:String, tpe:String) extends Resource
case class Repository(name:String, path:String) extends Resource
case class NotebookResource(name:String, path:String) extends Resource