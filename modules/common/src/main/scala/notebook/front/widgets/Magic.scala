package notebook.front.widgets.magic

import notebook.util.Reflector
import notebook.front.widgets.isNumber

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
//case class ChartPoint(x: Any, y: Any) extends MagicRenderPoint {
//  val X = "_1"
//  val Y = "_2"
//  val headers = Seq(X, Y)
//  def values  = Seq(x, y)
//}
//case class MapPoint(key: Any, value: Any) extends MagicRenderPoint {
//  val Key = "_1"
//  val Value = "_2"
//  val headers = Seq(Key, Value)
//  val values =  Seq(key, value)
//}
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
      case v: Any         => Reflector.toFieldValueArray(any)
  }
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

object Implicits extends ExtraMagicImplicits {
  trait ToPoints[C] {
    def apply(c:C, max:Int):Seq[MagicRenderPoint]
    def count(x:C):Long
    def append(x:C, y:C):C
  }
  implicit def SeqToPoints[T] = new ToPoints[Seq[T]] {
    def apply(_x:Seq[T], max:Int):Seq[MagicRenderPoint] =
      if (!_x.isEmpty) {
        val x = _x.take(max)
        val points:Seq[MagicRenderPoint] = x.head match {
          case _:String   => x.map(i => StringPoint(i.asInstanceOf[String]))
          case _:Graph[_] => x.map(_.asInstanceOf[Graph[_]].toPoint)
          case _          => x map AnyPoint
        }

        val encoded = if (x.head.isInstanceOf[Graph[_]]) {
                        points
                      } else {
                        points.zipWithIndex.map { case (point, index) => point.values match {
                          case List(o)    if isNumber(o)  =>  AnyPoint((index, o))
                          case _                          =>  point
                        }}
                      }
        encoded
      } else Nil
    def count(x:Seq[T]) = x.size.toLong
    def append(x:Seq[T], y:Seq[T]) = x ++ y
  }
  implicit def ListToPoints[T] = new ToPoints[List[T]] {
    def apply(x:List[T], max:Int):Seq[MagicRenderPoint] = SeqToPoints(x.take(max), max)
    def count(x:List[T]) = x.size.toLong
    def append(x:List[T], y:List[T]) = x ::: y
  }
  implicit def ArrayToPoints[T:scala.reflect.ClassTag] = new ToPoints[Array[T]] {
    def apply(x:Array[T], max:Int):Seq[MagicRenderPoint] = SeqToPoints(x.take(max).toSeq, max)
    def count(x:Array[T]) = x.size.toLong
    def append(x:Array[T], y:Array[T]) = (x ++ y).toArray
  }
  implicit def MapToPoints[K,V] = new ToPoints[Map[K,V]] {
    def apply(x:Map[K,V], max:Int):Seq[MagicRenderPoint] = SeqToPoints(x.take(max).toSeq, max)//x.toSeq.map(e => MapPoint(e._1, e._2))
    def count(x:Map[K,V]) = x.size.toLong
    def append(x:Map[K,V], y:Map[K,V]) = x ++ y
  }
}