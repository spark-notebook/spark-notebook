package notebook

import notebook.util.Match

import scala.reflect.internal.util.{SourceFile, OffsetPosition}
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.reporters.ConsoleReporter

class PresentationCompiler {
  def ask[T] (op: Response[T] => Unit) : Response[T] = {
    val r = new Response[T]
    op(r)
    r
  }

  val compiler: Global = {
    val target = new VirtualDirectory("", None)
    val s = new Settings() { usejavacp.value = true }
    s.outputDirs.setSingleOutput(target)

    val reporter = new ConsoleReporter(s)
    val compiler = new Global(s, reporter)
    compiler
  }

  val snippetPre = "import scala.collection._\nimport scala.math._\nimport scala.language._\n/*<imports>*/\nobject snippet {\ndef main() {\n"
  val snippedPost = "\n}\n}\n"

  @unchecked
  def wrapCodeWithMovedImports(code: String) : (String,Int) = {
    val codeLines = code.split(";|\n")
    val imports = codeLines.filter(c => c.startsWith("import")).mkString(";")
    val codeWithoutImports = codeLines.filter(c => !c.startsWith("import")).mkString(";")
    val snippetPreTemplate = snippetPre.replace("/*<imports>*/", imports)
    val wrappedCode = s"$snippetPreTemplate$codeWithoutImports$snippedPost"
    (wrappedCode,snippetPreTemplate.length)
  }

  def wrapCode(code: String) : (String,Int) = {
    val wrappedCode = s"$snippetPre$code$snippedPost"
    (wrappedCode,snippetPre.length)
  }

  def extractImports(code: String) : String = {
    val codeLines = code.split(";|\n")
    codeLines.filter(c => c.startsWith("import")).mkString(";")
  }

  case class CompletionInformation(symbol : String, parameters : String, symbolType: String)

  def completion(pos: OffsetPosition, filterSnippet: String,
                           op: (OffsetPosition,Response[List[compiler.Member]]) => Unit) : Seq[CompletionInformation] = {
    var result: Seq[CompletionInformation] = null
    val response = ask[List[compiler.Member]](op(pos, _))
    while (!response.isComplete && !response.isCancelled) {
      result = compiler.ask( () => {
        response.get(10) match {
          case Some(Left(t)) =>
            t .filter(m => m.sym.nameString.contains(filterSnippet) && m.accessible)
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
    val (wrappedCode,positionOffset) = wrapCode(code.substring(0,position))
    val filter1 = code.substring(0,position).split(";|\n", -1)
    var filterSnippet = ""
    //var isScopeCompletion = false
    if(filter1.nonEmpty) {
      val filter2 = filter1.last.split("[^a-zA-Z0-9_]+", -1)
      if(filter2.nonEmpty) {
        filterSnippet = filter2.last
      }
    }
    val source = compiler.newSourceFile(wrappedCode)
    reload(source)
    val pos = new OffsetPosition(source, position + positionOffset)
    var matches = completion (pos, filterSnippet, compiler.askTypeCompletion)
    if(matches.isEmpty) {
      matches = completion (pos, filterSnippet, compiler.askScopeCompletion)
    }
    val returnMatches = matches
      .map( m => Match(s"${m.symbol}", Map.empty[String,String]))
      .distinct
    (filterSnippet,returnMatches)
  }
}