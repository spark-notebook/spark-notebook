package notebook.kernel

import notebook.util.{Match, StringCompletor}

class TestStringCompletor extends StringCompletor {
  def complete(stringToComplete: String) = (stringToComplete, if (stringToComplete.toLowerCase.startsWith("usa")) Seq("usacpi", "usangdp").map(Match(_, Map("Location" -> "InflationEstimate/master"))) else Seq())
}
