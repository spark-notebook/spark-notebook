package notebook.front.widgets.magic

import notebook.util.Reflector
import notebook.front.widgets.isNumber
import org.apache.spark.sql.{Row}

import com.vividsolutions.jts.geom.Geometry
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
case class AnyPoint(any:Any) extends MagicRenderPoint {
  val headers = any match {
      case null           => Seq("Null")
      case v: Int         => Seq("Int")
      case v: Float       => Seq("Float")
      case v: Double      => Seq("Double")
      case v: Long        => Seq("Long")
      case v: BigDecimal  => Seq("BigDecimal")
      case v: String      => Seq("String")
      case v: Boolean     => Seq("Boolean")
      case v: Geometry    => Seq("Geometry")
      case v: GeoJSON     => Seq("GeoJSON")
      case v: Any         => Reflector.toFieldNameArray(any)
  }
  val values  = any match {
      case null           => Seq("<null-value>")
      case v: Int         => Seq(v)
      case v: Float       => Seq(v)
      case v: Double      => Seq(v)
      case v: Long        => Seq(v)
      case v: BigDecimal  => Seq(v)
      case v: String      => Seq(v)
      case v: Boolean     => Seq(v)
      case v: Geometry    => Seq(v)
      case v: GeoJSON     => Seq(v)
      case v: Any         => Reflector.toFieldValueArray(any)
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
case class Node[I](id:I, value:Any, color:String="black") extends Graph[I] {
  def toPoint:MagicRenderPoint = AnyPoint(value) merge StringPoint(id.toString, headers=Seq("nodeId")) merge StringPoint(color, headers=Seq("color"))
}
case class Edge[I](id:I, ends:(I, I), value:Any, color:String="#999") extends Graph[I] {
  def toPoint:MagicRenderPoint = AnyPoint(value) merge
                                  StringPoint(id.toString, headers=Seq("edgeId")) merge
                                  StringPoint(ends._1.toString, headers= Seq("end1Id")) merge
                                  StringPoint(ends._2.toString, headers= Seq("end2Id")) merge
                                  StringPoint(color, headers=Seq("color"))
}

object SamplerImplicits extends ExtraSamplerImplicits {
  trait Sampler[C] {
    def apply(c:C, max:Int):C
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
    def count(x:C):Long
    def append(x:C, y:C):C
    def mkString(x:C, sep:String=""):String
  }

  implicit def SeqToPoints[T] = new ToPoints[Seq[T]] {
    def apply(_x:Seq[T], max:Int)(implicit sampler:Sampler[Seq[T]]):Seq[MagicRenderPoint] =
      if (!_x.isEmpty) {
        val x = sampler(_x, max)
        val points:Seq[MagicRenderPoint] = x.head match {
          case _:Row   => x.map(i => SqlRowPoint(i.asInstanceOf[Row]))
          case _:String   => x.map(i => StringPoint(i.asInstanceOf[String]))
          case _:Graph[_] => x.map(_.asInstanceOf[Graph[_]].toPoint)
          case _          => x map AnyPoint
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
    def append(x:Array[T], y:Array[T]) = (x ++ y).toArray
    def mkString(x:Array[T], sep:String="") = x.mkString(sep)
  }
  implicit def VectorToPoints[T:scala.reflect.ClassTag] = new ToPoints[Vector[T]] {
    def apply(x:Vector[T], max:Int)(implicit sampler:Sampler[Vector[T]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max).toSeq, max)
    def count(x:Vector[T]) = x.size.toLong
    def append(x:Vector[T], y:Vector[T]) = (x ++ y)
    def mkString(x:Vector[T], sep:String="") = x.mkString(sep)
  }
  implicit def MapToPoints[K,V] = new ToPoints[Map[K,V]] {
    def apply(x:Map[K,V], max:Int)(implicit sampler:Sampler[Map[K,V]]):Seq[MagicRenderPoint] = SeqToPoints(sampler(x,max).toSeq, max)//x.toSeq.map(e => MapPoint(e._1, e._2))
    def count(x:Map[K,V]) = x.size.toLong
    def append(x:Map[K,V], y:Map[K,V]) = x ++ y
    def mkString(x:Map[K,V], sep:String="") = x.mkString(sep)
  }

}