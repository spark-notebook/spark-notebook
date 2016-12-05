package notebook

import java.util.Date

import play.api.libs.functional.syntax._
import play.api.libs.json._

import notebook.util.Logging

object NBSerializer extends Logging {
  trait Output {
    def output_type: String
  }

  case class ScalaOutput(
    name: String,
    output_type: String,
    prompt_number: Int,
    html: Option[String],
    text: Option[String]
  ) extends Output

  implicit val scalaOutputFormat = Json.format[ScalaOutput]

  case class ExecuteResultMetadata(id: Option[String] = None)

  implicit val executeResultMetadataFormat = Json.format[ExecuteResultMetadata]

  case class ScalaExecuteResult(
    metadata: ExecuteResultMetadata,
    data: Map[String, String],
    data_list: Option[Map[String, List[String]]],
    output_type: String,
    execution_count: Int,
    time: Option[String]
  ) extends Output

  implicit val scalaExecuteResultFormat = Json.format[ScalaExecuteResult]

  case class PyError(
    name: String,
    output_type: String,
    prompt_number: Int,
    traceback: String
  ) extends Output

  implicit val pyErrorFormat = Json.format[PyError]

  case class ScalaError(
    ename: String,
    output_type: String,
    traceback: List[String]
  ) extends Output
  implicit val scalaErrorFormat = Json.format[ScalaError]


  case class ScalaStream(name: String, output_type: String, text: String) extends Output

  implicit val scalaStreamFormat = Json.format[ScalaStream]


  implicit val outputReads: Reads[Output] = Reads { (js: JsValue) =>
    val tpe = (js \ "output_type").as[String]
    tpe match {
      case "execute_result" => scalaExecuteResultFormat.reads(js)
      case "stout" => scalaOutputFormat.reads(js)
      case "pyerr"  => pyErrorFormat.reads(js)
      case "error" => scalaErrorFormat.reads(js)
      case "stream" => scalaStreamFormat.reads(js)
      case x =>
        logError("Cannot read this output_type: " + x)
        throw new IllegalStateException("Cannot read this output_type: " + x)
    }
  }
  implicit val outputWrites: Writes[Output] = Writes { (o: Output) =>
    o match {
      case o: ScalaExecuteResult => scalaExecuteResultFormat.writes(o)
      case o: ScalaOutput => scalaOutputFormat.writes(o)
      case o: ScalaError => scalaErrorFormat.writes(o)
      case o: ScalaStream => scalaStreamFormat.writes(o)
    }
  }
  implicit val outputFormat: Format[Output] = Format(outputReads, outputWrites)

  case class CellMetadata(
    trusted: Option[Boolean],
    input_collapsed: Option[Boolean],
    output_stream_collapsed: Option[Boolean],
    collapsed: Option[Boolean],
    presentation: Option[JsObject],
    id: Option[String]=None,
    extra:Option[JsObject] = None
  )

  implicit val codeCellMetadataFormat = Json.format[CellMetadata]

  trait Cell {
    def metadata: CellMetadata

    def cell_type: String
  }

  case class CodeCell(
    metadata: CellMetadata,
    cell_type: String = "code",
    source: String,
    language: Option[String],
    prompt_number: Option[Int] = None,
    outputs: Option[List[Output]] = None
  ) extends Cell

  implicit val codeCellFormat = Json.format[CodeCell]

  case class MarkdownCell(
    metadata: CellMetadata,
    cell_type: String = "markdown",
    source: String
  ) extends Cell

  implicit val markdownCellFormat = Json.format[MarkdownCell]

  case class RawCell(metadata: CellMetadata, cell_type: String = "raw", source: String) extends Cell

  implicit val rawCellFormat = Json.format[RawCell]

  case class HeadingCell(
    metadata: CellMetadata,
    cell_type: String = "heading",
    source: String,
    level: Int
  ) extends Cell

  implicit val headingCellFormat = Json.format[HeadingCell]

  case class LanguageInfo(name: String, file_extension: String, codemirror_mode: String)

  implicit val languageInfoFormat: Format[LanguageInfo] = Json.format[LanguageInfo]
  val scala: LanguageInfo = LanguageInfo("scala", "scala", "text/x-scala")

  case class Metadata(
    name: String,
    user_save_timestamp: Date = new Date(0),
    auto_save_timestamp: Date = new Date(0),
    language_info: LanguageInfo = scala,
    trusted: Boolean = true,
    customLocalRepo: Option[String] = None,
    customRepos: Option[List[String]] = None,
    customDeps: Option[List[String]] = None,
    customImports: Option[List[String]] = None,
    customArgs: Option[List[String]] = None,
    customSparkConf: Option[JsObject] = None
  )

  implicit val metadataFormat: Format[Metadata] = {
    val f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val r: Reads[Metadata] = (
      (JsPath \ "name").read[String] and
        (JsPath \ "user_save_timestamp").read[String].map(x => f.parse(x)) and
        (JsPath \ "auto_save_timestamp").read[String].map(x => f.parse(x)) and
        (JsPath \ "language_info").readNullable[LanguageInfo].map(_.getOrElse(scala)) and
        (JsPath \ "trusted").readNullable[Boolean].map(_.getOrElse(true)) and
        (JsPath \ "customLocalRepo").readNullable[String] and
        (JsPath \ "customRepos").readNullable[List[String]] and
        (JsPath \ "customDeps").readNullable[List[String]] and
        (JsPath \ "customImports").readNullable[List[String]] and
        (JsPath \ "customArgs").readNullable[List[String]] and
        (JsPath \ "customSparkConf").readNullable[JsObject]
      )(Metadata.apply _)

    val w: Writes[Metadata] =
      OWrites { (m: Metadata) =>
        val name = JsString(m.name)
        val user_save_timestamp = JsString(f.format(m.user_save_timestamp))
        val auto_save_timestamp = JsString(f.format(m.auto_save_timestamp))
        val language_info = languageInfoFormat.writes(m.language_info)
        val trusted = JsBoolean(m.trusted)
        Json.obj(
          "name" → name,
          "user_save_timestamp" → user_save_timestamp,
          "auto_save_timestamp" → auto_save_timestamp,
          "language_info" → language_info,
          "trusted" → trusted,
          "customLocalRepo" → m.customLocalRepo,
          "customRepos" → m.customRepos,
          "customDeps" → m.customDeps,
          "customImports" → m.customImports,
          "customArgs" → m.customArgs,
          "customSparkConf" → m.customSparkConf
        )
      }

    Format(r, w)
  }

  implicit val cellReads: Reads[Cell] = Reads { (js: JsValue) =>
    val tpe = (js \ "cell_type").as[String]
    tpe match {
      case "code" => codeCellFormat.reads(js)
      case "markdown" => markdownCellFormat.reads(js)
      case "raw" => rawCellFormat.reads(js)
      case "heading" => headingCellFormat.reads(js)
      case x =>
        logError("Cannot read this cell_type: " + x)
        throw new IllegalStateException("Cannot read this cell_type: " + x)
    }
  }
  implicit val cellWrites: Writes[Cell] = Writes { (c: Cell) =>
    c match {
      case c: CodeCell => codeCellFormat.writes(c)
      case c: MarkdownCell => markdownCellFormat.writes(c)
      case c: RawCell => rawCellFormat.writes(c)
      case c: HeadingCell => headingCellFormat.writes(c)
    }
  }
  implicit val cellFormat: Format[Cell] = Format(cellReads, cellWrites)

  case class Worksheet(cells: List[Cell])

  implicit val worksheetFormat = Json.format[Worksheet]

  case class Notebook(
    metadata: Option[Metadata] = None,
    cells: Option[List[Cell]] = Some(Nil),
    worksheets: Option[List[Worksheet]] = None,
    autosaved: Option[List[Worksheet]] = None,
    nbformat: Option[Int]) {
    def name = metadata.map(_.name).getOrElse("Anonymous")
  }

  implicit val notebookFormat = Json.format[Notebook]

  def fromJson(json: JsValue): Option[Notebook] = {
    logTrace("\r\n**************************************\r\n")
    logTrace(Json.prettyPrint(json))
    logTrace("**************************************\r\n")
    json.validate[Notebook] match {
      case s: JsSuccess[Notebook] => {
        s.get match {
          case Notebook(None,None,None,None,None) =>
            logWarn("Nothing in the notebook data.")
            None
          case notebook =>
            Some( notebook.cells.map { _ => notebook } getOrElse notebook.copy(cells = Some(Nil)) )
        }
      }
      case e: JsError => {
        val ex = new RuntimeException(Json.stringify(JsError.toFlatJson(e)))
        logError("parse notebook", ex)
        throw ex
      }
    }
  }

  def read(s: String): Option[ Notebook ] = {
    fromJson(Json.parse(s))
  }

  def write(n: Notebook): String = {
    Json.prettyPrint(notebookFormat.writes(n))
  }

}
