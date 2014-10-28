package notebook

import org.json4s.JsonAST.JValue

package object util {
  implicit class GetRidOfWarningsInCompileForJValue(self:JValue) extends JValueWithFilter(self, _ => true)
}