package notebook.front.widgets.magic

import org.apache.spark.sql.{DataFrame, Row}

import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.isNumber

trait ExtraSamplerImplicits {
  import SamplerImplicits.Sampler

  /**
   * count number of rows in a DataFrame, and select first N rows if needed
   */
  implicit def LimitBasedDataFrameSampler = new Sampler[DataFrame] {
    override def samplingStrategy = new LimitBasedSampling()

    def apply(df: DataFrame, max: Int): DataFrame = {
      df.limit(max).cache()
    }
  }
}

trait ExtraMagicImplicits {
  case class DFPoint(row:Row, columns: Seq[String]) extends MagicRenderPoint {
    val headers = columns
    val values  = row.toSeq
  }

  implicit object DFToPoints extends ToPoints[DataFrame] {
    import SamplerImplicits.Sampler
    def apply(df:DataFrame, max:Int)(implicit sampler:Sampler[DataFrame]):Seq[MagicRenderPoint] = {
      val columns = df.columns.toSeq
      val rows = sampler(df, max).collect
      if (rows.length > 0) {
        val points = rows.map(i => DFPoint(i, columns))
        val encoded = points.zipWithIndex.map { case (point, index) => point.values match {
          case Seq(o)    if isNumber(o)   =>  AnyPoint((index, o))
          case _                          =>  point
        }}
        encoded
      } else Nil
    }
    override def headers(df: DataFrame)(implicit sampler:Sampler[DataFrame]) = df.columns
    def count(x:DataFrame) = x.count()
    def append(x:DataFrame, y:DataFrame) = x unionAll y
    def mkString(x:DataFrame, sep:String=""):String = x.rdd.toDebugString
  }
}