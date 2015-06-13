package notebook.util

import play.api.libs.json._

/**
 * Pluggable interface for completing Strings.
 */

case class Match(matchedValue: String, metadata: Map[String, String]) {
  def toJson = JsString(matchedValue)
}

object Match {
  def apply(matchedValue: String): Match = Match(matchedValue, Map())
}

trait StringCompletor {
  def complete(stringToComplete: String): (String, Seq[Match])
}
