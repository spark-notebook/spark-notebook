package com.bwater.notebook.widgets.d3

import com.bwater.notebook._, widgets._, JSBus._
import com.bwater.notebook.JsonCodec._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

trait DataProvider[T] {
  implicit val codec:Codec[JValue, T]

  val dataConnection:ValueConnection = JSBus.createConnection
  lazy val currentData:Connection[Seq[T]] = dataConnection biMap tSeq[T]

  def apply(newData: Seq[T]) = currentData <-- Connection.just(newData)
}

class Svg[T] (
    data: Seq[T],
    width: Int = 600,
    height: Int = 400,
    onData: String,
    extension: String
  )(implicit val codec:Codec[JValue, T])
  extends Widget with DataProvider[T] {

  private val js = List("sandbox", onData, extension).map(x => s"'js/$x'").mkString("[", ",", "]")
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
        s"require($js, $call);",
        ("dataId" -> dataConnection.id) ~
        ("dataInit" -> JsonCodec.tSeq[T].decode(data))
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
  )(implicit codec:Codec[JValue, T]):Svg[T] = new Svg(data, width, height, onData, extension)

  def linePlot[T](
    data: Seq[T],
    width: Int = 600,
    height: Int = 400
  )(implicit codec:Codec[(Double, Double), T]):Svg[T] = {
    val c = new Codec[JValue, T] {
      def encode(x:JValue):T = codec.encode(pair.encode(x))
      def decode(x:T):JValue = pair.decode(codec.decode(x))
    }
    new Svg(data, width, height, "linePlot", "consoleDir")(c)
  }


}
