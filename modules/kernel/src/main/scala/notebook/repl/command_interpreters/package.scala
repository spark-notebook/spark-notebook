package notebook.repl

import notebook.OutputTypes._
import notebook.client.ExecuteRequest
import notebook.repl.command_interpreters.CommandIntepreterType
import notebook.util.Logging

import scala.util.matching.Regex


case class ReplCommand(outputType: String, replCommand: String)

// A Helper to easily define classes implementing interpreter
// It needs to override a partial function that can be defined using
// the { case x: ExecuteRequest =>  ... } syntaxtic sugar
abstract class CommandIntepreter extends PartialFunction[ExecuteRequest, ReplCommand] {
  def delegate: PartialFunction[ExecuteRequest, ReplCommand]
  override def apply(s: ExecuteRequest): ReplCommand = delegate(s)
  override def isDefinedAt(x: ExecuteRequest): Boolean = delegate.isDefinedAt(x)
}

// a helper for matching code only: { case cellCode: String =>  ... }
abstract class CodeMatchingCommandInterpretter extends CommandIntepreter {
  def codeMatcher: PartialFunction[String, ReplCommand]

  override val delegate: CommandIntepreterType = new PartialFunction[ExecuteRequest, ReplCommand] {
    override def apply(s: ExecuteRequest): ReplCommand = codeMatcher(s.code)
    override def isDefinedAt(x: ExecuteRequest): Boolean = codeMatcher.isDefinedAt(x.code)
  }
}

package object command_interpreters {
  type CommandIntepreterType = PartialFunction[ExecuteRequest, ReplCommand]
  type CodeMatcherType = PartialFunction[String, ReplCommand]

  class ShellCommand extends CodeMatchingCommandInterpretter with Logging {
    private val shRegex = "(?s)^:sh\\s*(.+)\\s*$".r

    override val codeMatcher: CodeMatcherType = {
      case shRegex(sh) =>
        val ps = "s\"\"\"" + sh.replaceAll("\\s*\\|\\s*", "\" #\\| \"").replaceAll("\\s*&&\\s*", "\" #&& \"") + "\"\"\""

        val shCode =
          s"""|import sys.process._
              |println($ps.!!(ProcessLogger(out => (), err => println(err))))
              |()
              |""".stripMargin.trim
        logDebug(s"Generated SH code: $shCode")
        ReplCommand(`text/plain`, shCode)
    }
  }

  class SqlCommand extends CodeMatchingCommandInterpretter with Logging {
    private val sqlRegex = "(?s)^:sql(?:\\[([a-zA-Z0-9][a-zA-Z0-9]*)\\])?\\s*(.+)\\s*$".r

    override val codeMatcher: CodeMatcherType = {
      case sqlRegex(n, sql) =>
        logDebug(s"Received sql code: [$n] $sql")
        val qs = "\"\"\""
        val name = Option(n).map(nm => s"@transient val $nm = ").getOrElse("")
        ReplCommand(`text/html`,
          s"""
             |import notebook.front.widgets.Sql
             |import notebook.front.widgets.Sql._
             |${name}new Sql(sqlContext, s$qs$sql$qs)
             |""".stripMargin
        )
    }
  }

  class DefaultTextHtmlInterpreter extends CodeMatchingCommandInterpretter {
    override val codeMatcher: CodeMatcherType = {
      case code => ReplCommand(`text/html`, code)
    }
  }

  class RemovedCommandsInfo extends CodeMatchingCommandInterpretter with Logging {
    private val repoRegex = "(?s)^:local-repo\\s*(.+)\\s*$".r
    private val resolverRegex = "(?s)^:remote-repo\\s*(.+)\\s*$".r
    private val cpRegex = "(?s)^:cp\\s*(.+)\\s*$".r
    private val dpRegex = "(?s)^:(l?)dp\\s*(.+)\\s*$".r

    override val codeMatcher: CodeMatcherType = {
      case repoRegex(_) | resolverRegex(_) | cpRegex(_) | dpRegex(_, _) =>
        ReplCommand(`text/plain`,
          s"""
             |println("This command was removed. Please use 'Edit -> Notebook metadata' menu instead.")
             |println("The later is more stable and faster.")
             |println("After editing, restart the kernel and check your browser console for logs/errors.")
             |""".stripMargin
        )
    }
  }

  /**
    * Recognizes code prefixes related to outputType like this:
    *
    * :prefix
    * code
    *
    */
  case class OutputTypeMarker(prefix: String, outputType: String){
    val regex: Regex = s"(?s)^:$prefix\\s*\n(.+)\\s*$$".r

    def matches(code: String) = regex.findFirstIn(code).isDefined

    def genCode(origCode: String) = origCode
  }

  object OutputTypeCommand {
    /**
      * given String or scala.xml.Elem return a scala.xml.Elem
      */
    private def transformToXML(origCode: String) =
      s"""
         |def parseToXml(html: Any): scala.xml.Elem = {
         |  html match {
         |    case x: String => scala.xml.XML.loadString(x)
         |    case x: scala.xml.Elem => x
         |    case _ => throw new Exception("Can not display these outputs as XML/HTML")
         |  }
         |}
         |parseToXml({
         |  ${origCode}
         |})
         |""".stripMargin

    val scalaResultMarkers = Seq(
      OutputTypeMarker("markdown", `text/markdown`),
      OutputTypeMarker("latex", `text/latex`),
      OutputTypeMarker("png", `image/png`),
      OutputTypeMarker("jpeg", `image/jpeg`),
      OutputTypeMarker("pdf", `application/pdf`),

      // html/svg requires output to be scala.xml.Elem, so apply this transformation
      new OutputTypeMarker("html", `text/html`){ override def genCode(code: String) = transformToXML(code) },
      new OutputTypeMarker("svg", `image/svg+xml`){ override def genCode(code: String) = transformToXML(code) }
    )

    val javascript = OutputTypeMarker("javascript", `application/javascript`)
  }

  class OutputTypeCommand extends CodeMatchingCommandInterpretter {
    import OutputTypeCommand._

    override val codeMatcher: CodeMatcherType = {
      // e.g.
      // :marker
      // some_scala_code
      case cellContents if scalaResultMarkers.exists(_.matches(cellContents)) =>
        val matchingMarker = scalaResultMarkers.filter(_.matches(cellContents)).head
        val replCode = cellContents match { case matchingMarker.regex(scalaCode) =>
          matchingMarker.genCode(scalaCode)
        }
        ReplCommand(matchingMarker.outputType, replCode)

      // its less likely to would generate javascript, so it makes sense to access raw javascript code (?)
      case javascript.regex(content) =>
        val c = content.toString //.replaceAll("\"", "\\\"")
        ReplCommand(javascript.outputType, " s\"\"\"" + c + "\"\"\" ")
    }
  }

  def defaultInterpreters: Seq[CommandIntepreterType] = {
    Seq(
      new ShellCommand,
      new SqlCommand,
      new OutputTypeCommand,
      new RemovedCommandsInfo,
      new DefaultTextHtmlInterpreter
    )
  }

  def combineIntepreters(interpreters: Seq[CommandIntepreterType]): CommandIntepreterType = {
    interpreters.reduceLeft (_ orElse _)
  }
}
