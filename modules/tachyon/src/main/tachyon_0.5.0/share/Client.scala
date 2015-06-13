package notebook.share

import tachyon.client.TachyonFS

import scala.collection.JavaConverters._

class Client(tachyonUrl: Option[String]) {

  lazy val tachyonClient = tachyonUrl.map(
    url => TachyonFS.get(url)).getOrElse(notebook.share.Tachyon.fs)

  def list(path: String) = tachyonClient.ls(path, false).asScala
}