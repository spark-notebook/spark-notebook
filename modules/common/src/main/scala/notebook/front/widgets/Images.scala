package notebook.front.widgets

import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import notebook._
import notebook.front._
import notebook.JsonCodec._
import notebook.front.widgets.magic
import notebook.front.widgets.magic._

trait Images extends Generic with Utils {
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
                |req(
                |['observable', 'knockout'],
                |function (O, ko) {
                |  ko.applyBindings({
                |      value: O.makeObservable(valueId)
                |    },
                |    this
                |  );
                |});
            """.stripMargin,
            Json.obj("valueId" -> dataConnection.id)
          )
        }
      </p>

      def url(u:java.net.URL) = apply(ImageIO.read(u))
      def file(f:java.io.File) = apply(ImageIO.read(f))

  }
}