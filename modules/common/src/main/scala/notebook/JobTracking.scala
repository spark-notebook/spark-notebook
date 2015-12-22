package notebook

/**
  * Spark JobGroups are used to track which jobs are associated to which cells
  * - jobGroupID maps to 'cell-id'
  * - description contains a code-snippet
  */
object JobTracking {
  def jobGroupId(cellId: String) = s"cell-$cellId"

  def jobDescription(cellCode: String) = {
    cellCode.replaceAll("[^a-zA-Z0-9 ;_-]", "").take(255)
  }

  def toCellId(jobGroupId: Option[String]): Option[String] = {
    jobGroupId.map(_.replace("cell-", ""))
  }
}
