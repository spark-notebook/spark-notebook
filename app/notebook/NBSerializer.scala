package notebook

import java.util.Date

import play.api.Logger
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

  trait Cell {
    def cell_type:String
  }
  case class CodeCell(cell_type:String="code", input: String, language: String, collapsed: Boolean,prompt_number:Option[Int], outputs: List[Output]) extends Cell
  implicit val codeCellFormat = Json.format[CodeCell]
  case class MarkdownCell(cell_type:String="markdown", source: String) extends Cell
  implicit val markdownCellFormat = Json.format[MarkdownCell]
  case class RawCell(cell_type:String="raw", source: String) extends Cell
  implicit val rawCellFormat = Json.format[RawCell]
  case class HeadingCell(cell_type:String="heading", source: String, level: Int) extends Cell
  implicit val headingCellFormat = Json.format[HeadingCell]

  case class Metadata(name: String, user_save_timestamp: Date = new Date(0), auto_save_timestamp: Date = new Date(0))
  implicit val metadataFormat:Format[Metadata] = {
    val f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val r:Reads[Metadata] = (
      (JsPath \ "name").read[String] and
      (JsPath \ "user_save_timestamp").read[String].map(x => f.parse(x)) and
      (JsPath \ "auto_save_timestamp").read[String].map(x => f.parse(x))
    )(Metadata.apply _)

    val w:Writes[Metadata] =
      OWrites{ (m:Metadata) =>
        val name = JsString(m.name)
        val user_save_timestamp = JsString(f.format(m.user_save_timestamp))
        val auto_save_timestamp = JsString(f.format(m.auto_save_timestamp))
        Json.obj(
          "name" → name,
          "user_save_timestamp" → user_save_timestamp,
          "auto_save_timestamp" → auto_save_timestamp
        )
      }

    Format(r, w)
  }

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

  case class Notebook(metadata: Metadata, worksheets: List[Worksheet], autosaved: Option[List[Worksheet]]=None, nbformat: Option[Int]) {
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
        val ex = new RuntimeException(Json.stringify(JsError.toFlatJson(e)))
        Logger.error("parse notebook", ex)
        throw ex
      }
    }
  }

  def read(s:String):Notebook = {
    //Logger.info("Reading Notebook")
    //Logger.info(s)
    val json:JsValue = Json.parse(s)
    fromJson(json)
  }

  def write(n:Notebook):String = {
    Json.prettyPrint(notebookFormat.writes(n))
  }

}