package notebook.util

import org.json4s.JsonAST.{JString, JField, JObject}

/**
 * Pluggable interface for completing Strings.
 */

case class Match(matchedValue: String, metadata: Map[String, String]) {
  def toJson = JObject(JField("completion", JString(matchedValue)) :: (metadata.map { case (name, value) => JField(name, JString(value)) } toList))
}

object Match {
  def apply(matchedValue: String): Match = Match(matchedValue, Map())
}

trait StringCompletor {
  def complete(stringToComplete: String): (String, Seq[Match])
}
