package notebook.export

import java.io.File

import notebook.NBSerializer.{CodeCell, MarkdownCell, Output, ScalaExecuteResult, ScalaOutput, ScalaStream}
import notebook.Notebook

object Markdown {
  def toBq(s:String) = s.split("\n").map(s => s"> $s").mkString("\n")

  def bq(s:String) =
    s"""|
    |><pre>
    |${toBq(s)}
    |> </pre>
    |""".stripMargin

  def outputsToMarkdown(os:Option[List[Output]], dir:Option[File]):(String, Option[List[File]]) = {
    val files = new scala.collection.mutable.ArrayBuffer[File]()
    val outputs = os.getOrElse(Nil).collect {

      case ScalaOutput(_, _, _, html, text) =>
        text.map(s => s"$s\n\n\n").getOrElse("") + html.getOrElse("")

      case scalaExecResult: ScalaExecuteResult =>
        val dataList = scalaExecResult.data_list.getOrElse(Map.empty[String, List[String]])
        dataList.collect {
          case ("application/svg+pngbase64", ml) =>
            ml.map { m =>
              dir match {
                case Some(d) =>
                  val fl = d.list()
                  val i = if (Option(fl).filter(_.length > 0).isEmpty) 0 else fl.count(_.endsWith(".png"))
                  val imageFile = new File(dir.get, s"image-$i.png")
                  val imageString = m.dropWhile(_ != ',').tail

                  import org.apache.commons.codec.binary.Base64
                  val imageByte = Base64.decodeBase64(imageString)
                  val bis = new java.io.ByteArrayInputStream(imageByte)

                  val image = javax.imageio.ImageIO.read(bis);

                  bis.close();

                  javax.imageio.ImageIO.write(image, "png", imageFile);

                  files += imageFile

                  s"> ![generated image $i](./${dir.get.getName}/${imageFile.getName})"
                case None =>
                  s"> <img src='$m'/>"
              }
            }
        }.flatten.mkString("\n\n")

      case ScalaStream(_, _, d) => bq(d)

    }.mkString("\n")
    outputs -> (if (files.nonEmpty) Some(files.toList) else None)
  }

  // FIXME: might require refactoring
  def isNonEmptyCodeCell(cell: CodeCell) =  {
    val codeCellLanguages = Seq(None, Some("scala"))
    cell.cell_type == "code" &&
      cell.sourceString.trim.nonEmpty &&
      codeCellLanguages.contains(cell.language)
  }

  def generate(nb:Notebook, nbPath:String, single:Boolean):Option[Either[String, File]] = {
    // make sure file names dont contain funny symbols
    val name = nbPath.replace("/", "_")
    val (dir,images) = if (!single) {
      val dir = new File(sys.props("java.io.tmpdir"), name+"-"+System.nanoTime)
      dir.mkdir
      val images = new File(dir, "images")
      images.mkdir
      (Some(dir), Some(images))
    } else { (None, None) }

    nb.cells.map { cells =>
      val csFiles:List[(String, Option[List[File]])]= cells.collect {
        case cell: CodeCell if isNonEmptyCodeCell(cell) =>
          val source = cell.sourceString

          val (t, code) = if (source.startsWith(":sh")) {
            ("sh", source.drop(3))
          } else {
            ("scala", source)
          }

          val (outputsMarkdown, files) = outputsToMarkdown(cell.outputs, images)
          s"""
          |```$t
          |$code
          |```
          |
          |$outputsMarkdown
          |""".stripMargin -> files

        case MarkdownCell(_, _, i)  if i.trim.nonEmpty => i -> None
      }
      val (cs, filesList) = csFiles.map(x => (x._1, x._2.getOrElse(Nil))).unzip
      val files = filesList.flatten
      val fc = cs.mkString("\n").trim
      if (single) {
        Left(fc)
      } else {
        val mdFile = new File(dir.get, name+".md")
        mdFile.createNewFile
        val w = new java.io.FileWriter(mdFile)
        w.write(fc)
        w.close

        val zipFile = new File(dir.get, name+".zip")
        zipFile.createNewFile
        val baos = new java.io.FileOutputStream(zipFile);
        val zip = new java.util.zip.ZipOutputStream(baos);

        def write(files:List[File], prefix:String) = files.foreach { f =>
          zip.putNextEntry(new java.util.zip.ZipEntry(prefix+f.getName))
          val in = new java.io.BufferedInputStream(new java.io.FileInputStream(f))
          var b = in.read()
          while (b > -1) {
            zip.write(b)
            b = in.read()
          }
          in.close()
          zip.closeEntry()
        }

        write(List(mdFile), "")
        write(files, "images/")

        zip.close()

        Right(zipFile)
      }
    }
  }
}
