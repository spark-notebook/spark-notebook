package com.bwater.notebook
package server

import org.fusesource.scalate.{
  TemplateEngine, Binding, DefaultRenderContext, RenderContext}
import unfiltered.request.{Path,HttpRequest}
import unfiltered.response.{ResponseWriter}
import java.io.{File,OutputStreamWriter,PrintWriter}
import scala.util.control.NonFatal

object Scalate {
  /** Constructs a ResponseWriter for Scalate templates.
   *  Note that any parameter in the second, implicit set
   *  can be overriden by specifying an implicit value of the
   *  expected type in a pariticular scope. */
  def apply[A, B](request: HttpRequest[A],
                  template: String,
                  attributes:(String,Any)*)
  ( implicit
    engine: TemplateEngine = defaultEngine,
    contextBuilder: ToRenderContext = defaultRenderContext,
    bindings: List[Binding] = Nil,
    additionalAttributes: Seq[(String, Any)] = Nil
  ) = new ResponseWriter {
    def write(writer: OutputStreamWriter) {
      val printWriter = new PrintWriter(writer)
      try {
        val scalateTemplate = engine.load(template, bindings)
        val context = contextBuilder(Path(request), printWriter, engine)
        (additionalAttributes ++ attributes) foreach {
          case (k,v) => context.attributes(k) = v
        }
        engine.layout(scalateTemplate, context)
      } catch {
        case NonFatal(e) if engine.isDevelopmentMode =>
          printWriter.println("Exception: " + e.getMessage)
          e.getStackTrace.foreach(printWriter.println)
        case NonFatal(e) => throw e
      }
    }
  }

  /* Function to construct a RenderContext. */
  type ToRenderContext =
    (String, PrintWriter, TemplateEngine) => RenderContext

  private val defaultTemplateDirs =
    new File("src/main/resources/templates") :: Nil
  private val defaultEngine = new TemplateEngine(defaultTemplateDirs, "development")
  private val defaultRenderContext: ToRenderContext =
    (path, writer, engine) =>
      new DefaultRenderContext(path, engine, writer)
}
