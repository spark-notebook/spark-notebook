package notebook.front.widgets.magic

import java.sql.{Date => SqlDate, Timestamp => SqlTimestamp}
import java.text.SimpleDateFormat
import java.util.Date

import com.vividsolutions.jts.geom.Geometry
import notebook.front.widgets.isNumber
import notebook.util.Reflector
import org.apache.spark.sql.Row
import org.wololo.geojson.GeoJSON

trait MagicRenderPoint { me =>
  def headers:Seq[String]
  def numOfFields = headers.size
  def values:Seq[Any]
  def data:Map[String, Any] = headers zip values toMap

  def merge(m:MagicRenderPoint) = new MagicRenderPoint {
    val headers = me.headers ++ m.headers
    override val numOfFields = me.numOfFields + m.numOfFields
    val values = me.values ++ m.values
    override val data = me.data ++ m.data
  }
}
case class StringPoint(string:String, headers:Seq[String]=Seq("string value")) extends MagicRenderPoint {
  val values  = Seq(string)
}

case class AnyPoint(point:Any, header:Option[String]=None) extends MagicRenderPoint {

  private val dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z")

  override val headers = point match {
      case null           => Seq(header.getOrElse("Null"))
      case _: Int         => Seq(header.getOrElse("Int"))
      case _: Float       => Seq(header.getOrElse("Float"))
      case _: Double      => Seq(header.getOrElse("Double"))
      case _: Long        => Seq(header.getOrElse("Long"))
      case _: BigDecimal  => Seq(header.getOrElse("BigDecimal"))
      case _: String      => Seq(header.getOrElse("String"))
      case _: Boolean     => Seq(header.getOrElse("Boolean"))
      case _: SqlDate     => Seq(header.getOrElse("Date"))
      case _: SqlTimestamp => Seq(header.getOrElse("Timestamp"))
      case _: Date        => Seq(header.getOrElse("Date"))
      case _: Geometry    => Seq(header.getOrElse("Geometry"))
      case _: GeoJSON     => Seq(header.getOrElse("GeoJSON"))
      case _: Any         => Reflector.toFieldNameArray(point)
  }

  /**
    * Result of this conversion is displayed in widgets (like in [[notebook.front.widgets.charts.TableChart]] for
    * example. Thus we want to convert points containing dates into human readable format. Otherwise they would be
    * displayed as timestamps.
    *
    * This is also consumed by c3 library when plotting [[notebook.front.widgets.charts.TimeseriesChart]]. So string
    * format used here needs to be in sync with format used to parse this string there.
    */
  override val values  = point match {
      case null           => Seq("<null-value>")
      case v: Int         => Seq(v)
      case v: Float       => Seq(v)
      case v: Double      => Seq(v)
      case v: Long        => Seq(v)
      case v: BigDecimal  => Seq(v)
      case v: Date        => Seq(dateFormat.format(v))
      case v: String      => Seq(v)
      case v: Boolean     => Seq(v)
      case v: Geometry    => Seq(v)
      case v: GeoJSON     => Seq(v)
      case v: Any         => Reflector.toFieldValueArray(v).map {
        case v: Date => dateFormat.format(v)
        case fieldValue => fieldValue
      }
  }
}

case class SqlRowPoint(row: Row) extends MagicRenderPoint {
  val headers = row.schema.fieldNames.toSeq
  val values = headers.map(row.getAs[Any])
}

sealed trait Graph[I] {
  def id:I
  def color:String
  def toPoint:MagicRenderPoint
}
case class Node[I](id:I, value:Any, color:String="black", r:Int=5, xy:Option[(Double,Double)]=None, fixed:Boolean=false) extends Graph[I] {
  def toPoint:MagicRenderPoint = AnyPoint(value) merge
                                  StringPoint(id.toString, headers=Seq("nodeId")) merge
                                  StringPoint(color, headers=Seq("color")) merge
                                  AnyPoint(r, Some("r")) merge
                                  AnyPoint(xy.map(_._1).getOrElse(scala.util.Random.nextDouble), Some("x")) merge
                                  AnyPoint(xy.map(_._2).getOrElse(scala.util.Random.nextDouble), Some("y")) merge
                                  AnyPoint(fixed, Some("fixed"))
}
case class Edge[I](id:I, ends:(I, I), value:Any, color:String="#999") extends Graph[I] {
  def toPoint:MagicRenderPoint = AnyPoint(value) merge
                                  StringPoint(id.toString, headers=Seq("edgeId")) merge
                                  StringPoint(ends._1.toString, headers= Seq("end1Id")) merge
                                  StringPoint(ends._2.toString, headers= Seq("end2Id")) merge
                                  StringPoint(color, headers=Seq("color"))
}

sealed trait SamplingStrategy
case class LimitBasedSampling() extends SamplingStrategy
case class RandomSampling() extends SamplingStrategy

object SamplerImplicits extends ExtraSamplerImplicits {
  trait Sampler[C] {
    def apply(c:C, max:Int):C
    def samplingStrategy: SamplingStrategy = RandomSampling()
  }

  implicit def SeqSampler[T] = new Sampler[Seq[T]] {
    def apply(x:Seq[T], max:Int):Seq[T] = x.take(max)
  }
  implicit def ListSampler[T] = new Sampler[List[T]] {
    def apply(x:List[T], max:Int):List[T] = x.take(max)
  }
  implicit def ArraySampler[T] = new Sampler[Array[T]] {
    def apply(x:Array[T], max:Int):Array[T] = x.take(max)
  }
  implicit def MapSampler[K,V] = new Sampler[Map[K, V]] {
    def apply(x:Map[K,V], max:Int):Map[K,V] = x.toSeq.take(max).toMap
  }
}

object Implicits extends ExtraMagicImplicits {
  import SamplerImplicits.Sampler

  trait ToPoints[C] {
    def apply(c:C, max:Int)(implicit sampler:Sampler[C]):Seq[MagicRenderPoint]

    /**
      * Dummy implementation which tries to get the fetch schema/headers from the first element
      * to be overriden by specific converters.
      */
    def headers(x: C)(implicit sampler:Sampler[C]) = {
      apply(x, max = 1)
        .headOption
        .map(_.headers)
        .getOrElse(Seq("can not display empty seq"))
    }
    def count(x:C):Long
    def append(x:C, y:C):C
    def mkString(x:C, sep:String=""):String
  }

  implicit def SeqToPoints[T] = new ToPoints[Seq[T]] {
    def apply(_x:Seq[T], max:Int)(implicit sampler:Sampler[Seq[T]]):Seq[MagicRenderPoint] =
      if (_x.nonEmpty) {
        val x = sampler(_x, max)
        val points:Seq[MagicRenderPoint] = x.head match {
          case _:Row   => x.map(i => SqlRowPoint(i.asInstanceOf[Row]))
          case _:String   => x.map(i => StringPoint(i.asInstanceOf[String]))
          case _:Graph[_] => x.map(_.asInstanceOf[Graph[_]].toPoint)
          case _          => x map (x => AnyPoint(x))
        }

        val encoded = if (x.head.isInstanceOf[Graph[_]]) {
                        points
                      } else {
                        points.zipWithIndex.map { case (point, index) => point.values match {
                          case Seq(o)    if isNumber(o)  =>  AnyPoint((index, o))
                          case _                          =>  point
                        }}
                      }
        encoded
      } else Nil
    def count(x:Seq[T]) = x.size.toLong
    def append(x:Seq[T], y:Seq[T]) = x ++ y
    def mkString(x:Seq[T], sep:String="") = x.mkString(sep)
  }
  implicit def ListToPoints[T] = new ToPoints[List[T]] {
    def apply(x:List[T], max:Int)(implicit sampler:Sampler[List[T]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max), max)
    def count(x:List[T]) = x.size.toLong
    def append(x:List[T], y:List[T]) = x ::: y
    def mkString(x:List[T], sep:String="") = x.mkString(sep)
  }
  implicit def ArrayToPoints[T:scala.reflect.ClassTag] = new ToPoints[Array[T]] {
    def apply(x:Array[T], max:Int)(implicit sampler:Sampler[Array[T]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max).toSeq, max)
    def count(x:Array[T]) = x.size.toLong
    def append(x:Array[T], y:Array[T]) = x ++ y
    def mkString(x:Array[T], sep:String="") = x.mkString(sep)
  }
  implicit def VectorToPoints[T:scala.reflect.ClassTag] = new ToPoints[Vector[T]] {
    def apply(x:Vector[T], max:Int)(implicit sampler:Sampler[Vector[T]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max), max)
    def count(x:Vector[T]) = x.size.toLong
    def append(x:Vector[T], y:Vector[T]) = x ++ y
    def mkString(x:Vector[T], sep:String="") = x.mkString(sep)
  }
  implicit def MapToPoints[K,V] = new ToPoints[Map[K,V]] {
    def apply(x:Map[K,V], max:Int)(implicit sampler:Sampler[Map[K,V]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max).toSeq, max)//x.toSeq.map(e => MapPoint(e._1, e._2))
    def count(x:Map[K,V]) = x.size.toLong
    def append(x:Map[K,V], y:Map[K,V]) = x ++ y
    def mkString(x:Map[K,V], sep:String="") = x.mkString(sep)
  }
}