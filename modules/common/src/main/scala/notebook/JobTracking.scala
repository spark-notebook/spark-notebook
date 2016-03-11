package notebook

/**
  * Spark JobGroups are used to track which jobs are associated to which cells
  * - jobGroupID maps to 'cell-id'
  * - description contains cell start time & code-snippet
  */
object JobTracking {
  def jobGroupId(cellId: String) = s"cell-$cellId"

  def jobDescription(cellCode: String, runId: Long) = {
    val codeSnipped = cellCode.replace("\"", "'").replaceAll("[^a-zA-Z0-9 ;:'*._={}\\-()]", "").take(255)
    s"run-$runId: $codeSnipped"
  }

  def getCellRunId(jobDescription: Option[String]) = {
    jobDescription.map(_.split(":").head.replace("run-", "").toLong)
  }

  def toCellId(jobGroupId: Option[String]): Option[String] = {
    jobGroupId.map(_.replace("cell-", ""))
  }
}
