  package notebook.util

import org.json4s.JsonAST.{JString, JField}
import org.json4s._

class JValueWithFilter(self: JValue, p: JValue => Boolean) {
  def map[T](f: JValue => T): List[T] =
    self.filter(p).map(f)
  def flatMap[T](f: JValue => List[T]): List[T] =
    self.filter(p).flatMap(f)
  def foreach(f: JValue => Unit): Unit =
    self.filter(p).foreach(f)
  def withFilter(q: JValue => Boolean): JValueWithFilter =
    new JValueWithFilter(self, x => p(x) && q(x))
}