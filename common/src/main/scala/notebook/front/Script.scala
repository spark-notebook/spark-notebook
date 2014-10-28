package notebook.front

import org.json4s.JsonAST._
import org.json4s.native._

case class Script(script:String, options:JObject) {
  val name = "_"+script.replaceAll("[^_a-zA-Z0-9]", "")
  val toJson = s"""{
    "f": $name,
    "o": ${ prettyJson(renderJValue(options)) }
  }
  """
}