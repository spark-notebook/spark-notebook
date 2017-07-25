package com.fasterxml.jackson.core.util

import java.io.{IOException, Writer}

import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator}
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import play.api.libs.json.JsObject
import play.api.libs.json.jackson.PlayJsonModule

// based on https://stackoverflow.com/questions/14938667/jackson-json-deserialization-array-elements-in-each-line
// and source of play.libs.Json.prettyPrint / PlayJson/ Jackson

private object PrettyPrinter {
  val instance = new DefaultPrettyPrinter()
  instance.indentArraysWith(new DefaultIndenter)
}

private class Factory extends JsonFactory {
  @throws[IOException]
  override protected def _createGenerator(out: Writer, ctxt: IOContext): JsonGenerator = {
    super._createGenerator(out, ctxt)
      .setPrettyPrinter(PrettyPrinter.instance)
  }
}

object JsonPrettyPrintHacks {
  def prettyPrintArrays(jsValue: JsObject): String = {
    val mapper = new ObjectMapper(new Factory()).registerModule(PlayJsonModule)
    mapper.setDefaultPrettyPrinter(PrettyPrinter.instance)
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    writer.writeValueAsString(jsValue)
  }
}
