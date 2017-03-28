import Dependencies._
import sbt.Keys._
import sbt._

import scala.collection.mutable.ListBuffer

// These act as "containers" of settings of each module,
// so the settings can be applied dynamically
object Extra {
  val sparkNotebookSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val sparkNotebookCoreSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val gitNotebookProviderSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val sbtDependencyManagerSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val subprocessSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val observableSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val commonSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val sparkSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
  val kernelSettings: ListBuffer[Def.Setting[_]] = ListBuffer()
}
