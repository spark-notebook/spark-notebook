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

  val snippetObjectImports = Seq(
    "import java.io.{File, FileReader, BufferedReader}",
    "import notebook._",
    "import notebook.front._",
    "import notebook.front.widgets._",
    "import notebook.front.third.d3._",
    "import notebook.front.widgets.magic._",
    "import notebook.front.widgets.magic.Implicits._",
    "import notebook.JsonCodec._",
    "import org.apache.spark.{SparkContext, SparkConf}",
    "import org.apache.spark.SparkContext._",
    "import org.apache.spark.rdd._",
    "import scala.collection._",
    "import scala.math._",
    "import scala.language._",
    "/*<imports>*/"
  )

  val snippetPre = snippetObjectImports.mkString("\n") + "\nobject snippet {\ndef main() {\n"
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

  def completion(pos: OffsetPosition, op: (OffsetPosition,Response[List[compiler.Member]]) => Unit) : Seq[CompletionInformation] = {
    var result: Seq[CompletionInformation] = null
    val response = ask[List[compiler.Member]](op(pos, _))
    while (!response.isComplete && !response.isCancelled) {
      result = compiler.ask( () => {
        response.get(10) match {
          case Some(Left(t)) =>
            t .map( m => CompletionInformation(
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
    val (wrappedCode,positionOffset) = wrapCode(code) //.substring(0,position))
    val filter1 = code.substring(0,position).split(";|\n", -1)
    var filterSnippet = ""
    if(filter1.nonEmpty) {
      val filter2 = filter1.last.split("[^a-zA-Z0-9_]+", -1)
      if(filter2.nonEmpty) {
        filterSnippet = filter2.last
      }
    }
    val source = compiler.newSourceFile(wrappedCode)
    reload(source)
    val pos = new OffsetPosition(source, position + positionOffset)
    var matches = completion (pos, compiler.askTypeCompletion)
    if(matches.isEmpty) {
      matches = completion (pos, compiler.askScopeCompletion)
    }
    val returnMatches = matches
      .filter(m => m.symbol.startsWith(filterSnippet))
      .map( m => if (m.symbol == filterSnippet) Match(s"${m.symbol}(${m.parameters})", Map()) else Match(s"${m.symbol}", Map()))
      .distinct
    (filterSnippet,returnMatches)
  }
}