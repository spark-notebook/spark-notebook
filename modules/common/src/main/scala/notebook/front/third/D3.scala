package notebook.front.third.d3

import notebook._, front._
import notebook.front.widgets._

import notebook._, JSBus._
import notebook.JsonCodec._

import play.api.libs.json._

class Svg[T] (
    data: Seq[T],
    width: Int = 600,
    height: Int = 400,
    onData: String,
    extension: String
  )(implicit val singleCodec:Codec[JsValue, T])
  extends Widget with DataConnector[T] {

  private val js = List("sandbox", onData, extension).map(x => s"'../javascripts/notebook/$x'").mkString("[", ",", "]")
  private val call = if (onData == extension) {
    s"""
      function(s, onData) {
        this.onData = onData;
        this.extension = onData;
        s.call(data, this, onData, onData);
      }
    """
  } else {
    s"""
      function(sandbox, onData, extension) {
        sandbox.call(data, this, onData, extension);
      }
    """
  }

  lazy val toHtml =
    <svg class="d3 plot" width={ width.toString } height={ height.toString }
       xmlns="http://www.w3.org/2000/svg" version="1.1">
    {
      scopedScript(
        s"req($js, $call);",
        Json.obj(
          ("dataId" -> dataConnection.id),
          ("dataInit" -> JsonCodec.tSeq[T].decode(data))
        )
      )
    } </svg>
}

object D3 {

  def svg[T](
    data: Seq[T],
    width: Int = 600,
    height: Int = 400,
    onData: String,
    extension: String
  )(implicit codec:Codec[JsValue, T]):Svg[T] = new Svg(data, width, height, onData, extension)

  def linePlot[T](
    data: Seq[T],
    width: Int = 600,
    height: Int = 400
  )(implicit codec:Codec[(Double, Double), T]):Svg[T] = {
    val c = new Codec[JsValue, T] {
      def encode(x:JsValue):T = codec.encode(pair.encode(x))
      def decode(x:T):JsValue = pair.decode(codec.decode(x))
    }
    new Svg(data, width, height, "linePlot", "consoleDir")(c)
  }


}
