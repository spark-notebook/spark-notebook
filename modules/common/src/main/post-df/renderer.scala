package notebook.front

import org.apache.spark.sql.DataFrame

trait ExtraLowPriorityRenderers {

  import widgets._

  implicit object dataFrameAsTable extends Renderer[DataFrame] {
    def render(x: DataFrame) = x.take(1) match {
      case x if x.isEmpty => widgets.layout(0, Seq(widgets.text("empty data frame")))
      case _ => display(x)
    }
  }

}