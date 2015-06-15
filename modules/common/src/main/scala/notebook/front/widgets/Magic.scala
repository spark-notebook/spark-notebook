package notebook.front.widgets.magic

import notebook.util.Reflector
import notebook.front.widgets.isNumber

trait MagicRenderPoint {
  def headers:Seq[String]
  def numOfFields = headers.size
  def values:Seq[Any]
  def data:Map[String, Any] = headers zip values toMap
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
case class StringPoint(string:String) extends MagicRenderPoint {
  val headers = Seq("string value")
  val values  = Seq(string)
}
case class AnyPoint(any:Any) extends MagicRenderPoint {
  val headers = Reflector.toFieldNameArray(any)
  val values  = Reflector.toFieldValueArray(any)
}

object Implicits extends ExtraMagicImplicits {
  trait ToPoints[C] {
    def apply(c:C, max:Int):Seq[MagicRenderPoint]
    def count(x:C):Long
  }
  implicit def SeqToPoints[T] = new ToPoints[Seq[T]] {
    def apply(x:Seq[T], max:Int):Seq[MagicRenderPoint] =
      if (!x.isEmpty) {
        val points = x.head match {
          case _:String => x.map(i => StringPoint(i.asInstanceOf[String]))
          case _        => x.map(i => AnyPoint(i))
        }

        val encoded = points.zipWithIndex.map { case (point, index) => point.values match {
          case List(o)    if isNumber(o)  =>  ChartPoint(index, o)
          case List(a, b)                 =>  ChartPoint(a, b)
          case _                          =>  point
        }}
        encoded
      } else Nil
    def count(x:Seq[T]) = x.size.toLong
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