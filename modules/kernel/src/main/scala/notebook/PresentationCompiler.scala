package notebook

import notebook.util.Match

import scala.reflect.internal.util.{SourceFile, OffsetPosition}
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.reporters.ConsoleReporter

class PresentationCompiler {
  trait Probe

  def ask[T] (op: Response[T] => Unit) : Response[T] = {
    val r = new Response[T]
    op(r)
    r
  }

  val compiler: Global = {
    val target = new VirtualDirectory("", None)
    val s = new Settings() { usejavacp.value = true }
    s.embeddedDefaults[Probe]
    s.outputDirs.setSingleOutput(target)
    s.usejavacp.value = true
    s.dependenciesFile.tryToSet(
      List(
        "scala-library.jar",
        "scala-compiler.jar",
        "scala-reflect.jar"
      )
    )

    val reporter = new ConsoleReporter(s)
    val compiler = new Global(s, reporter)
    compiler
  }

  protected val snippetPre = "import scala.collection._;import scala.math._;import scala.language._; object snippet { def main() { "
  protected val snippedPost = " } }"

  protected def wrapCode(code: String) : String = {
    val codeWithoutLinebreaks = code.replace("\n", ";")
    val wrappedCode = s"$snippetPre$codeWithoutLinebreaks$snippedPost"
    wrappedCode
  }

  protected def computeCodeCompletionIndex(code: String, position: Int) : (Int, String) = {
    val lastPeriodIndex = code.substring(0,position).lastIndexOf('.') + 1
    val alreadyTypedInCodeCompletionSnippet = code.substring(lastPeriodIndex,position)
    (lastPeriodIndex, alreadyTypedInCodeCompletionSnippet)
  }

  case class CompletionInformation(symbol : String, parameters : String, symbolType: String)

  protected def completion(pos: OffsetPosition, filterSnippet: String,
                           op: (OffsetPosition,Response[List[compiler.Member]]) => Unit) : Seq[CompletionInformation] = {
    var result: Seq[CompletionInformation] = null
    val response = ask[List[compiler.Member]](op(pos, _))
    while (!response.isComplete && !response.isCancelled) {
      result = compiler.ask( () => {
        response.get(10) match {
          case Some(Left(t)) =>
            t .filter(m => m.sym.nameString.startsWith(filterSnippet))
              .map( m => CompletionInformation(
                  m.sym.nameString,
                  m.tpe.params.map( p => s"<${p.name.toString}:${p.tpe.toString()}>").mkString(", "),
                  ""//m.tpe.toString()
                )
              )
              .toSeq
          case Some(Right(ex)) =>
            ex.printStackTrace()
            println(ex)
            Seq.empty[CompletionInformation]
          case None => Seq.empty[CompletionInformation]
        }
      })
    }
    result
  }

  def reload(source: SourceFile) : Unit = {
    ask[Unit](compiler.askReload(List(source), _)).get
  }

  def complete(code: String, position: Int) : (String, Seq[Match]) = {
    val wrappedCode = wrapCode(code)
    val source = compiler.newSourceFile(wrappedCode)
    reload(source)
    val (realPosition, filterSnippet) = computeCodeCompletionIndex(wrappedCode, position + snippetPre.length)
    val pos = new OffsetPosition(source, realPosition)
    val matches = completion(pos, filterSnippet,compiler.askTypeCompletion)
    val exactMatches = matches
      .filter( m => m.symbol == filterSnippet )
      .map( m => Match(s"${m.symbol}(${m.parameters})", Map.empty[String,String]))
    val otherMatches = matches
      .filter( m => m.symbol != filterSnippet )
      .map( m => Match(m.symbol, Map.empty[String,String]))
    val returnMatches = exactMatches.union(otherMatches).distinct
    (filterSnippet,returnMatches)
  }
}