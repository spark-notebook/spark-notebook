package notebook.front.widgets.magic

import org.apache.spark.sql.{DataFrame, Row}

import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.isNumber

trait ExtraMagicImplicits {
  case class DFPoint(row:Row, df:DataFrame) extends MagicRenderPoint {
    val headers = df.columns.toSeq
    val values  = row.toSeq
  }

  implicit object DFToPoints extends ToPoints[DataFrame] {
    def apply(df:DataFrame, max:Int):Seq[MagicRenderPoint] = {
      val rows = df.take(max)
      if (rows.nonEmpty) {
        val points = df.schema.toList.map(_.dataType) match {
          case List(x) if x.isInstanceOf[org.apache.spark.sql.types.StringType] => rows.map(i => StringPoint(i.asInstanceOf[String]))
          case _                                                                => rows.map(i => DFPoint(i, df))
        }

        val encoded = points.zipWithIndex.map { case (point, index) => point.values match {
          case List(o)    if isNumber(o)  =>  ChartPoint(index, o)
          case List(a, b)                 =>  ChartPoint(a, b)
          case _                          =>  point
        }}
        encoded
      } else Nil
    }
    def count(x:DataFrame) = x.count()
  }
}