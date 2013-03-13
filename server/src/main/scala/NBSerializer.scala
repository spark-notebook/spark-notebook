package com.bwater.notebook

import net.liftweb.json._
import net.liftweb.json.Serialization
import java.util.{Date}

/**
 * Author: Ken
 * The core object model. Fields are pythonic naming convention to match javascript
 */


object NBSerializer {
  trait Output
  case class ScalaOutput(prompt_number: Int, html: Option[String], text: Option[String]) extends Output
  case class ScalaError(prompt_number: Int, traceback: String) extends Output
  case class ScalaStream(text: String, stream: String) extends Output

  trait Cell
  case class CodeCell(input: String, language: String, collapsed: Boolean,prompt_number:Option[Int], outputs: List[Output]) extends Cell
  case class MarkdownCell(source: String) extends Cell
  case class RawCell(source: String) extends Cell
  case class HeadingCell(source: String, level: Int) extends Cell
  case class Metadata(name: String, user_save_timestamp: Date, auto_save_timestamp: Date)  {
    def this(name: String) = this(name, new Date(0), new Date(0))
  }
  case class Worksheet(cells: List[Cell])
  case class Notebook(metadata: Metadata, worksheets: List[Worksheet], autosaved: List[Worksheet], nbformat: Option[Int]) {
    def name = metadata.name
  }

  // Short type hints for inner classes of this class
  case class NBTypeHints(hints: List[Class[_]]) extends TypeHints {
    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf("$")+1)
    def classFor(hint: String) = hints find (hintFor(_) == hint)
  }


  implicit val formats = Serialization.formats(NBTypeHints(List(classOf[CodeCell], classOf[MarkdownCell], classOf[RawCell], classOf[HeadingCell], classOf[ScalaOutput], classOf[ScalaError], classOf[ScalaStream])))
  val translations = List(
    ("cell_type", "code", "CodeCell"),
    ("cell_type", "markdown", "MarkdownCell"),
    ("cell_type", "raw", "RawCell"),
    ("cell_type", "heading", "HeadingCell"),
    ("output_type", "pyout", "ScalaOutput"),
    ("output_type", "pyerr", "ScalaError"),
    ("output_type", "stream", "ScalaStream")
  )

  def write(nb: Notebook): String = {
    val json = Extraction.decompose(nb)

    val mapped = json transform {
      case JField("jsonClass", JString(x)) =>
        val (typ, cat, _) =
          (translations filter { _._3 == x }).head
        JField(typ, JString(cat))
      }
    pretty(render(mapped))
  }

  def read(s: String): Notebook = {
    val json = parse(s)
    val mapped = json transform {
      case JField(typ, JString(cat)) if (translations exists { x => x._1 == typ && x._2 == cat }) =>
        val (_, _, clazz) = (translations filter { x => x._1 == typ && x._2 == cat }).head
        JField("jsonClass", JString(clazz))
    }
    mapped.extract[Notebook]
  }

}
//  def reads(s: String) : Notebook = {
//    val json = parse(s)
//    val res = for {
//      JObject(child) <- json
//      JField("name", JString(name)) <- child
//      JField("worksheets", JArray(sheet :: Nil)) <- child
//      JObject(ws) <- sheet
//      JField("cells", JArray(cells)) <- ws
//    } yield Notebook(name, List(Worksheet(cells map parseCell)))
//
//    res.head
//
//  }
//
//  def parseCell(lst: JValue) = {
//    lst match {
//      case JObject if (obj.values("cell_type") == "code") =>
//        val JField("collapsed", JBool(collapsed)) =
//        val JField("input", JString(input)) = child
//        Code(input, collapsed, Nil)
//    }
//    val JObject(child) = lst
//    child match {
//      case JField("cell_type", JString("code")) =>
//    }
//  }
//}
//object Notebook {
//  case class LCTypeHints(hints: List[Class[_]]) extends TypeHints {
//    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf(".")+1).toLowerCase
//    def classFor(hint: String) = hints find (hintFor(_) == hint)
//  }
//
//  val cellSerializer = new TypeHints extends FieldSerializer[Cell](
//    FieldSerializer.renameTo("cell_type", "jsonClass"),
//    FieldSerializer.renameFrom("jsonClass", "cell_type"))
//  val outSerializer = FieldSerializer[Cell](
//    FieldSerializer.renameTo("output_type", "jsonClass"),
//    FieldSerializer.renameFrom("jsonClass", "output_type"))
//
//
//   val formats = Serialization.formats(LCTypeHints(List(classOf[Code], classOf[Markdown]))
//}
//
