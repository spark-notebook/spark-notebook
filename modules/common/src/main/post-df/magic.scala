package notebook.front.widgets.magic

import org.apache.spark.sql.{DataFrame, Row}

import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.LimitBasedSampling
import notebook.front.widgets.isNumber

trait ExtraSamplerImplicits {
  import SamplerImplicits.Sampler

  /**
   * count number of rows in a DataFrame, and select first N rows if needed
   */
  implicit def DummyDataFrameSampler = new Sampler[DataFrame] {
    override def samplingStrategy = new LimitBasedSampling()

    def apply(df: DataFrame, max: Int): DataFrame = {
      val firstRows = df.limit(max).cache()
      if (firstRows.count() > max) {
        println(s"DF is larger than maxRows. Showing only first ${max} rows.")
        firstRows
      } else {
        df
      }
    }
  }
}

trait ExtraMagicImplicits {
  case class DFPoint(row:Row, df:DataFrame) extends MagicRenderPoint {
    val headers = df.columns.toSeq
    val values  = row.toSeq
  }

  implicit object DFToPoints extends ToPoints[DataFrame] {
    import SamplerImplicits.Sampler
    def apply(df:DataFrame, max:Int)(implicit sampler:Sampler[DataFrame]):Seq[MagicRenderPoint] = {
      if (df.take(1).nonEmpty) {
        val rows = sampler(df, max).collect
        val points = df.schema.toList.map(_.dataType) match {
          case List(x) if x.isInstanceOf[org.apache.spark.sql.types.StringType] => rows.map(i => StringPoint(i.asInstanceOf[String]))
          case _                                                                => rows.map(i => DFPoint(i, df))
        }

        val encoded = points.zipWithIndex.map { case (point, index) => point.values match {
          case Seq(o)    if isNumber(o)   =>  AnyPoint((index, o))
          case _                          =>  point
        }}
        encoded
      } else Nil
    }
    def count(x:DataFrame) = x.count()
    def append(x:DataFrame, y:DataFrame) = x unionAll y
    def mkString(x:DataFrame, sep:String=""):String = x.rdd.toDebugString
  }
}