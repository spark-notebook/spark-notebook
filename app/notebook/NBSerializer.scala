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

  case class Notebook(metadata: Metadata, worksheets: List[Worksheet], autosaved: Option[List[Worksheet]], nbformat: Option[Int]) {
    def name = metadata.name
  }
  implicit val notebookFormat = Json.format[Notebook]

  def fromJson(json:JsValue):Notebook = {
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

  def read(s:String):Notebook = {
    val json:JsValue = Json.parse(s)
    fromJson(json)
  }

  def write(n:Notebook):String = {
    Json.prettyPrint(notebookFormat.writes(n))
  }

}