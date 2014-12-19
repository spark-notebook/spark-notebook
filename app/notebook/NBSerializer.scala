package notebook

import java.util.Date

import play.api.libs.json._
import play.api.libs.functional.syntax._

object NBSerializer {
  trait Output
  case class ScalaOutput(prompt_number: Int, html: Option[String], text: Option[String]) extends Output
  implicit val scalaOutputFormat = Json.format[ScalaOutput]
  case class ScalaError(prompt_number: Int, traceback: String) extends Output
  implicit val scalaErrorFormat = Json.format[ScalaError]
  case class ScalaStream(text: String, stream: String) extends Output
  implicit val scalaStreamFormat = Json.format[ScalaStream]


  implicit val outputReads:Reads[Output] = Reads { (js:JsValue) =>
    val tpe = (js \ "output_type").as[String]
    tpe match {
      case "pyout"  => scalaOutputFormat.reads(js)
      case "pyerr"  => scalaErrorFormat.reads(js)
      case "stream" => scalaStreamFormat.reads(js)
    }
  }
  implicit val outputWrites:Writes[Output] = Writes { (o:Output) =>
    o match {
      case o:ScalaOutput => scalaOutputFormat.writes(o)
      case o:ScalaError  => scalaErrorFormat.writes(o)
      case o:ScalaStream => scalaStreamFormat.writes(o)
    }
  }
  implicit val outputFormat:Format[Output] = Format(outputReads, outputWrites)

  trait Cell
  case class CodeCell(input: String, language: String, collapsed: Boolean,prompt_number:Option[Int], outputs: List[Output]) extends Cell
  implicit val codeCellFormat = Json.format[CodeCell]
  case class MarkdownCell(source: String) extends Cell
  implicit val markdownCellFormat = Json.format[MarkdownCell]
  case class RawCell(source: String) extends Cell
  implicit val rawCellFormat = Json.format[RawCell]
  case class HeadingCell(source: String, level: Int) extends Cell
  implicit val headingCellFormat = Json.format[HeadingCell]

  case class Metadata(name: String, user_save_timestamp: Date = new Date(0), auto_save_timestamp: Date = new Date(0))
  implicit val hetadataFormat = Json.format[Metadata]


  implicit val cellReads:Reads[Cell] = Reads { (js:JsValue) =>
    val tpe = (js \ "cell_type").as[String]
    tpe match {
      case "code"     => codeCellFormat.reads(js)
      case "markdown" => markdownCellFormat.reads(js)
      case "raw"      => rawCellFormat.reads(js)
      case "heading"  => headingCellFormat.reads(js)
    }
  }
  implicit val cellWrites:Writes[Cell] = Writes { (c:Cell) =>
    c match {
      case c:CodeCell     => codeCellFormat.writes(c)
      case c:MarkdownCell => markdownCellFormat.writes(c)
      case c:RawCell      => rawCellFormat.writes(c)
      case c:HeadingCell  => headingCellFormat.writes(c)
    }
  }
  implicit val cellFormat:Format[Cell] = Format(cellReads, cellWrites)

  case class Worksheet(cells: List[Cell])
  implicit val worksheetFormat = Json.format[Worksheet]

  case class Notebook(metadata: Metadata, worksheets: List[Worksheet], autosaved: List[Worksheet], nbformat: Option[Int]) {
    def name = metadata.name
  }
  implicit val notebookFormat = Json.format[Notebook]

  def read(s:String):Notebook = {
    val json:JsValue = Json.parse(s)
    json.validate[Notebook] match {
      case s: JsSuccess[Notebook] => {
        val notebook:Notebook = s.get
        notebook
      }
      case e: JsError => {
        throw new RuntimeException(Json.stringify(JsError.toFlatJson(e)))
      }
    }
  }

  def write(n:Notebook):String = {
    Json.prettyPrint(notebookFormat.writes(n))
  }

/*

  // Short type hints for inner classes of this class
  case class NBTypeHints(hints: List[Class[_]]) extends TypeHints {
    def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf("$")+1)
    def classFor(hint: String) = hints find (hintFor(_) == hint)
  }

  implicit val formats = Serialization.formats(NBTypeHints(List(classOf[CodeCell],
                                                                classOf[MarkdownCell],
                                                                classOf[RawCell],
                                                                classOf[HeadingCell],
                                                                classOf[ScalaOutput],
                                                                classOf[ScalaError],
                                                                classOf[ScalaStream])))
  val translations = List(
    ("cell_type",   "code",     "CodeCell"),
    ("cell_type",   "markdown", "MarkdownCell"),
    ("cell_type",   "raw",      "RawCell"),
    ("cell_type",   "heading",  "HeadingCell"),
    ("output_type", "pyout",    "ScalaOutput"),
    ("output_type", "pyerr",    "ScalaError"),
    ("output_type", "stream",   "ScalaStream")
  )

  def write(nb: Notebook): String = {
    val json = Extraction.decompose(nb)

    val mapped = json transformField {
      case JField("jsonClass", JString(x)) =>
        val (typ, cat, _) =
          (translations filter { _._3 == x }).head
        JField(typ, JString(cat))
      }
    prettyJson(renderJValue(mapped))
  }

  def read(s: String): Notebook = {
    val json = parseJson(s)
    val mapped = json transformField {
      case JField(typ, JString(cat)) if (translations exists { x => x._1 == typ && x._2 == cat }) =>
        val (_, _, clazz) = (translations filter { x => x._1 == typ && x._2 == cat }).head
        JField("jsonClass", JString(clazz))
    }
    mapped.extract[Notebook]
  }
*/
}