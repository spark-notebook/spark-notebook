package notebook.front

import xml.{NodeSeq, UnprefixedAttribute, Null}

import play.api.libs.json._

import notebook._, front._, widgets._
import notebook._, JSBus._
import JsonCodec._

/**
 * This package contains primitive widgets that can be used in the child environment.
 */
package object widgets {
  def scopedScript(content: String, data: JsValue = null, selector:Option[String]=None) = {
    val tag = <script type="text/x-scoped-javascript">/*{xml.PCData("*/" + content + "/*")}*/</script>
    val withData = if (data == null)
      tag
    else
      tag % new UnprefixedAttribute("data-this", Json.stringify(data), Null)

    val withSelector = selector.map { s =>
      withData % new UnprefixedAttribute("data-selector", s, Null)
    }.getOrElse(withData)



    withSelector
  }

  def text(value: String) = html(xml.Text(value))

  def text(value: Connection[String], style: Connection[String] = Connection.just("")) = {
    val _currentValue = JSBus.createConnection
    val stringCodec:Codec[JsValue, String] = formatToCodec(implicitly[Format[String]])
    val currentValue = _currentValue biMap stringCodec
    currentValue <-- value

    val _currentStyle = JSBus.createConnection
    val currentStyle = _currentStyle biMap stringCodec
    currentStyle <-- style

    html(<p data-bind="text: value, style: style">{
      scopedScript(
        """ req(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId),
                    style: O.makeObservable(styleId)
                  },
                  this
                );
              }
        """,
        Json.obj("valueId" -> _currentValue.id, "styleId" -> _currentStyle.id)
      )}</p>)
  }

  def ul(capacity:Int=10) = new DataConnectedWidget[String] {
    implicit val singleCodec:Codec[JsValue, String] = JsonCodec.strings

    var data = Seq.empty[String]

    lazy val toHtml = <ul data-bind="foreach: value">
      <li data-bind="text: $data"></li>{
        scopedScript(
          """
              req(
                ['observable', 'knockout'],
                function (O, ko) {
                  ko.applyBindings({
                      value: O.makeObservable(valueId)
                    },
                    this
                  );
                }
              );
          """,
          Json.obj("valueId" -> dataConnection.id)
        )
      }</ul>

    override def apply(d:Seq[String]) = {
      data = if (d.size > capacity) {
         d.drop(d.size - capacity)
      } else {
        d
      }
      super.apply(data)
    }

    def append(s:String) = {
      apply(data :+ s)
    }

    def appendAll(s:Seq[String]) = {
      apply(data ++ s)
    }
  }

  def out = new SingleConnectedWidget[String] {
    implicit val codec:Codec[JsValue, String] = formatToCodec(Format.of[String])

    lazy val toHtml = <p data-bind="text: value">{
      scopedScript(
        """
            req(
              ['observable', 'knockout'],
              function (O, ko) {
                ko.applyBindings({
                    value: O.makeObservable(valueId)
                  },
                  this
                );
              }
            );
        """,
        Json.obj("valueId" -> dataConnection.id)
      )}</p>
  }

  import java.awt.image.BufferedImage
  import java.io.ByteArrayOutputStream
  import javax.imageio.ImageIO

  def imageCodec(tpe:String) = new Codec[JsValue, BufferedImage] {
    def toBytes(bi:BufferedImage):String = {
      val bos = new ByteArrayOutputStream()
      ImageIO.write(bi, tpe, bos)
      val imageBytes = bos.toByteArray()
      val encodedImage = org.apache.commons.codec.binary.Base64.encodeBase64String(imageBytes)
      val imageString = "data:image/"+tpe+";base64,"+encodedImage
      bos.close()
      imageString
    }
    def decode(a: BufferedImage):JsValue = JsString(toBytes(a))
    def encode(v: JsValue):BufferedImage = ??? //todo
  }

  def img(tpe:String="png", width:String="150px", height:String="150px") = new SingleConnectedWidget[BufferedImage] {
    implicit val codec:Codec[JsValue, BufferedImage] = imageCodec(tpe)

    lazy val toHtml = <p>
      <img width={width} height={height} data-bind="attr:{src: value}" />
        {
          scopedScript(
            """
                req(
                  ['observable', 'knockout'],
                  function (O, ko) {
                    ko.applyBindings({
                        value: O.makeObservable(valueId)
                      },
                      this
                    );
                  }
                );
            """,
            Json.obj("valueId" -> dataConnection.id)
          )
        }
      </p>

      def url(u:java.net.URL) = apply(ImageIO.read(u))
      def file(f:java.io.File) = apply(ImageIO.read(f))

  }


  def html(html: NodeSeq): Widget = new SimpleWidget(html)

  def layout(width: Int, contents: Seq[Widget]): Widget = html(
    <table>{
      contents grouped width map { row =>
        <tr>{
          row map { html => <td>{html}</td> }
        }</tr>
      }
    }</table>)

  def row(contents: Widget*) = layout(contents.length, contents)
  def column(contents: Widget*) = layout(1, contents)

  def multi(widgets: Widget*) = html(NodeSeq.fromSeq(widgets.map(_.toHtml).flatten))
}