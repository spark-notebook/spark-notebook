package notebook.front.widgets.magic

import org.apache.spark.sql.{DataFrame, Row}

import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.isNumber

trait ExtraSamplerImplicits {
  import SamplerImplicits.Sampler
  /**
   * count number of rows in a DataFrame, and do sampling if needed
   */
  implicit def SimpleDataFrameSampler = new Sampler[DataFrame] {
    def apply(df: DataFrame, max: Int): DataFrame = {
      val count = df.cache().count()
      if (count > max) {
        println("DF is larger than maxRows. Will use sampling.")
        df.sample(withReplacement = false, max / count.toDouble)
          .cache()
      } else {
        df
      }
    }
  }

  implicit object DFSampler extends Sampler[DataFrame] {
    import org.apache.spark.sql.types.{StructType, StructField,LongType}
    import org.apache.spark.sql.functions._
    //until zipWithIndex will be available on DF
    def dfZipWithIndex(df: DataFrame):DataFrame = {
      df.sqlContext.createDataFrame(
        df.rdd.zipWithIndex.map(ln =>
          Row.fromSeq(
            ln._1.toSeq ++ Seq(ln._2)
          )
        ),
        StructType(
          df.schema.fields ++
          Array(StructField("_sn_index_",LongType,false))
        )
      )
    }
    def apply(df:DataFrame, max:Int):DataFrame = {
      import df.sqlContext.implicits._
      val columns = df.columns
      dfZipWithIndex(df).filter($"_sn_index_" < max)
                        .select(columns.head, columns.tail: _*)
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