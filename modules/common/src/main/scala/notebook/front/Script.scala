package notebook.front

import play.api.libs.json._

case class Script(script:String, options:JsObject) {
  val name = "_"+script.replaceAll("[^_a-zA-Z0-9]", "")
  val toJson = s"""{
    "f": $name,
    "o": ${ Json.stringify(options) }
  }
  """
}