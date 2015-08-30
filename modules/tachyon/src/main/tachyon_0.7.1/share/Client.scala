package notebook.share

import scala.collection.JavaConverters._
import tachyon.client.TachyonFS
import tachyon.conf.TachyonConf
import tachyon.TachyonURI

class Client(tachyonUrl:Option[String]) {

  lazy val tachyonClient = tachyonUrl.map{ url =>
                                        TachyonFS.get(new TachyonURI(url), new TachyonConf())
                                      }
                                      .getOrElse{
                                        notebook.share.Tachyon.fs
                                      }

  def list(path:String) = tachyonClient.listStatus(new tachyon.TachyonURI(path)).asScala.map(_.path)
}