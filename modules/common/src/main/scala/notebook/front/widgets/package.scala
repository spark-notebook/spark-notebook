package notebook.front

import scala.util.Random
import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import com.vividsolutions.jts.geom.Geometry
import org.wololo.geojson.GeoJSON
import notebook._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._
import notebook.front.widgets.magic.Implicits._
import notebook.front.widgets.magic.SamplerImplicits._

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets
  extends Generic
  with Texts
  with Lists
  with Layouts
  with charts.Charts {
}