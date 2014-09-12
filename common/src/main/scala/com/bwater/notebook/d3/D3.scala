package com.bwater.notebook.widgets.d3

import com.bwater.notebook._, widgets._
import com.bwater.notebook.JsonCodec._
import net.liftweb.json.JsonAST._
import net.liftweb.json.JsonDSL._

object D3 {
  def apply[T](
    data:Seq[T],
    width: Int = 600,
    height: Int = 400,
    onData: String,
    extension: String
  )(implicit codec:Codec[JValue, T]) = new Widget {
    private[this] val dataConnection = JSBus.createConnection
    lazy val currentData = dataConnection biMap tSeq

    val js = List("sandbox", onData, extension).map(x => s"'js/$x'").mkString("[", ",", "]")
    val call = if (onData == extension) {
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
          ("dataInit" -> JsonCodec.tSeq.decode(data)) //~
          //("onData" -> s"; var onData = $onData ; ") ~        //ouch non hygienic
          //("extension" -> s"; var extension = $extension ; ") //ouch non hygienic
        )
      } </svg>

    def apply(newData: Seq[T]) = currentData <-- Connection.just(newData)
  }

}
