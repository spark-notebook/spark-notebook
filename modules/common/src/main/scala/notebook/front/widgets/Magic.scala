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
case class ChartPoint(x: Any, y: Any) extends MagicRenderPoint {
  val X = "X"
  val Y = "Y"
  val headers = Seq(X, Y)
  def values  = Seq(x, y)
}
case class MapPoint(key: Any, value: Any) extends MagicRenderPoint {
  val Key = "Key"
  val Value = "Value"
  val headers = Seq(Key, Value)
  val values =  Seq(key, value)
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
  }
  implicit def SeqToPoints[T] = new ToPoints[Seq[T]] {
    def apply(x:Seq[T], max:Int):Seq[MagicRenderPoint] =
      if (!x.isEmpty) {
        val points:Seq[MagicRenderPoint] = x.head match {
          case _:String   => x.map(i => StringPoint(i.asInstanceOf[String]))
          case _:Graph[_] => x.map(_.asInstanceOf[Graph[_]].toPoint)
          case _          => x map AnyPoint
        }

        val encoded = if (x.head.isInstanceOf[Graph[_]]) {
                        points
                      } else {
                        points.zipWithIndex.map { case (point, index) => point.values match {
                          case List(o)    if isNumber(o)  =>  ChartPoint(index, o)
                          case List(a, b)                 =>  ChartPoint(a, b)
                          case _                          =>  point
                        }}
                      }
        encoded
      } else Nil
    def count(x:Seq[T]) = x.size.toLong
  }
  implicit def ListToPoints[T] = new ToPoints[List[T]] {
    def apply(x:List[T], max:Int):Seq[MagicRenderPoint] = SeqToPoints(x.take(max), max)
    def count(x:List[T]) = x.size.toLong
  }
  implicit def ArrayToPoints[T] = new ToPoints[Array[T]] {
    def apply(x:Array[T], max:Int):Seq[MagicRenderPoint] = SeqToPoints(x.take(max).toSeq, max)
    def count(x:Array[T]) = x.size.toLong
  }
  implicit def MapToPoints[K,V] = new ToPoints[Map[K,V]] {
    def apply(x:Map[K,V], max:Int):Seq[MagicRenderPoint] = x.toSeq.map(e => MapPoint(e._1, e._2))
    def count(x:Map[K,V]) = x.size.toLong
  }
}