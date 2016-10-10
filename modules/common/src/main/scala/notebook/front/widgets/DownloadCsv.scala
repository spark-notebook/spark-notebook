package notebook.front.widgets

import notebook.front.Widget
import org.apache.hadoop.fs.{FileUtil, FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.sql.DataFrame

case class DownloadCsv(df: DataFrame, webHdfsUserName: String = "hive") extends Widget {
  val log = org.slf4j.LoggerFactory.getLogger("SparkInfo")

  val link = SpreadsheetOutput.downloadCsv(df, webHdfsUserName = webHdfsUserName)

  lazy val toHtml = {
    <div class="download-csv">
      <a target="_blank" href={link}>Download here</a>
    </div>
  }
}

object SpreadsheetOutput {
  implicit class DataFrameWithSpreadsheetOutput(df: DataFrame) {
    def downloadCsv() = DownloadCsv(df)
  }

  implicit class SparkContextWithFsMerge(sc: SparkContext) {
    def mergeDirToSingleFile(srcDir: String, dstFile: String) = {
      val hadoopConfig = sc.hadoopConfiguration
      val fs = FileSystem.get(hadoopConfig)

      val srcPath = new Path(srcDir)
      val dstPath = new Path(dstFile)
      require(fs.exists(srcPath), s"Can not merge files of non existent dir: $srcPath")
      require(!fs.exists(dstPath), s"The destination path should not exist: $dstPath")

      FileUtil.copyMerge(
        fs, srcPath,
        fs, dstPath,
        /* deleteSource = */ true,
        /* conf = */ fs.getConf,
        /* addString = */ null
      )
    }
  }

  protected def tmpFileName() = s"/tmp/df-export-${java.util.UUID.randomUUID.toString}"

  def saveAsCsv(df: DataFrame, fileName: String) = {
    df.write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .save(fileName)
  }

  /**
    * @param hdfsPath - like /user/someuser/someFileName
    */
  def webHdfsDownloadLink(hdfsPath: String, userName: String = "hive") = {
    // operations described at https://goo.gl/i0bt6m
    sys.env.get("WEB_HDFS_URL") match {
      case Some(webHdfsHost) =>
        val webHdfs = s"${webHdfsHost}/webhdfs/v1"
        s"${webHdfs}${hdfsPath}?user.name=${userName}&op=open"
      case _ =>
        s"hdfs://${hdfsPath}"
    }
  }

  def downloadCsv(df: DataFrame, fileName: String = "download.csv", webHdfsUserName: String = "hive"): String = {
    val temporaryFileName = s"${tmpFileName()}/$fileName"

    // save as regular hadoop dir with one part-00000 file
    val temporaryDirName = s"${temporaryFileName}-tmpdir"

    // df.repartition(100) needed so calculation would not happen on single node
    // (df.coalesce(1) alone causes exception even on moderate input size)
    saveAsCsv(df.repartition(100).coalesce(1), temporaryDirName)

    // merge files in temporaryDirName into a single file which can be downloaded
    df.sqlContext.sparkContext.mergeDirToSingleFile(temporaryDirName, temporaryFileName)

    webHdfsDownloadLink(temporaryFileName, webHdfsUserName)
  }
}
