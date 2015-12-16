package notebook

import notebook.util.Match

import scala.collection.mutable.ArrayBuffer
import scala.reflect.internal.util.{SourceFile, OffsetPosition}
import scala.reflect.io.VirtualDirectory
import scala.tools.nsc.Settings
import scala.tools.nsc.interactive.{Global, Response}
import scala.tools.nsc.reporters.ConsoleReporter

class PresentationCompiler(dependencies: List[String]) {

  val scripts : ArrayBuffer[String] = ArrayBuffer.empty[String]

  def addScripts(code: String) : Unit = {
    scripts.+=(code)
  }

  def ask[T] (op: Response[T] => Unit) : Response[T] = {
    val r = new Response[T]
    op(r)
    r
  }

  val compiler: Global = {
    val target = new VirtualDirectory("", None)
    val s = new Settings() { usejavacp.value = true }
    s.outputDirs.setSingleOutput(target)
    dependencies.foreach(s.classpath.append)
    dependencies.foreach(s.bootclasspath.append)
    val reporter = new ConsoleReporter(s)
    val compiler = new Global(s, reporter)
    compiler
  }

  def snippetPre() = "\nobject snippet {\n" + scripts.mkString("\n") + "\n"
  val snippedPost = "\n}\n"

  def wrapCode(code: String) : (String,Int) = {
    val wrappedCode = s"${snippetPre()}$code$snippedPost"
    (wrappedCode,snippetPre().length)
  }

  case class CompletionInformation(symbol : String, parameters : String, returnType: String){
    def nameAndParams = if (parameters.isEmpty) {
      symbol
    } else {
      s"${symbol}(${parameters})"
    }

    def nameParamsAndReturnType = s"${nameAndParams}: ${returnType}"
  }

  def completion(pos: OffsetPosition, op: (OffsetPosition,Response[List[compiler.Member]]) => Unit) : Seq[CompletionInformation] = {
    var result: Seq[CompletionInformation] = null
    val response = ask[List[compiler.Member]](op(pos, _))
    while (!response.isComplete && !response.isCancelled) {
      result = compiler.ask( () => {
        response.get(10) match {
          case Some(Left(t)) =>
            t .map( m => CompletionInformation(
                  m.sym.nameString,
                  m.tpe.params.map( p => s"${p.name.toString}: ${p.tpe.toString()}").mkString(", "),
                  m.tpe.resultType.toString
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
    val returnMatches: Seq[Match] = matches
      .filter(m => m.symbol.startsWith(filterSnippet))
      .map {
        // if text already matches the method-name, autocomplete all matching method versions (name, params, returnType)
        case m if m.symbol == filterSnippet =>
          val metadata = Map("display_text" -> m.nameParamsAndReturnType, "dedup_key" -> m.nameParamsAndReturnType)
          Match(m.nameAndParams, metadata)
        // otherwise, autocomplete only method name, but we'll display the first matching signature
        case m =>
          Match(m.symbol, Map("display_text" -> m.nameParamsAndReturnType, "dedup_key" -> m.symbol))
      }
      .groupBy(_.metadata("dedup_key"))
      .map {
        case (_, matches) if matches.size > 1 =>
          val m = matches.head
          val updatedDisplayText = s"${m.metadata("display_text")} [multiple versions]"
          Match(m.matchedValue, m.metadata.updated("display_text", updatedDisplayText))
        case (_, matches) => matches.head
      }.toSeq

    (filterSnippet, returnMatches)
  }
}
