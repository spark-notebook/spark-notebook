package notebook.front

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{SQLContext, DataFrame, Dataset}

import scala.reflect.runtime.universe.TypeTag

trait ExtraLowPriorityRenderers {

  import widgets._

  implicit object dataFrameAsTable extends Renderer[DataFrame] {
    def render(x: DataFrame) = new DatasetWidget(x, 25, "consoleDir")
  }

  implicit def datasetAsTable[T] = new Renderer[Dataset[T]] {
    def render(x: Dataset[T]) = new DatasetWidget(x, 25, "consoleDir")
  }
}
