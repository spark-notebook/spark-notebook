package notebook.front

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, DataFrame}

import scala.reflect.runtime.universe.TypeTag

trait ExtraLowPriorityRenderers {

  import widgets._

  implicit object dataFrameAsTable extends Renderer[DataFrame] {
    def render(x: DataFrame) = new DataFrameWidget(x, 25, "consoleDir")
  }
}