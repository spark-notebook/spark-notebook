package utils

import akka.actor._
import notebook.server._

object AppUtils {

  import play.api.Play.current

  lazy val config = NotebookConfig(current.configuration.getConfig("manager").get)
  lazy val nbm = new NotebookManager(config.projectName, config.notebooksDir)
  lazy val clustersConf = config.config.getConfig("clusters").get

  lazy val nbServerConf = current.configuration.getConfig("notebook-server").get.underlying
  lazy val kernelSystem = ActorSystem(
    "NotebookServer",
    nbServerConf,
    play.api.Play.classloader // todo more detail, this resolves the Play classloader problems w/ remoting
  )

  object proxy {
    def proxyKeys(pre: String) = List(
      pre + "_proxy",
      pre + "_proxy_user",
      pre + "_proxy_pass",
      pre + ".proxyHost",
      pre + ".proxyPort",
      pre + ".proxyUser",
      pre + ".proxyPassword",
      pre + ".nonProxyHosts"
    )

    def mapper = (path:String) => path → current.configuration.getString(path)
    def collecter:PartialFunction[(String, Option[String]),(String, String)] = { case (x:String, Some(y:String)) => x → y }
    //Proxy
    val http = proxyKeys("http") map mapper collect collecter
    val https = proxyKeys("https") map mapper collect collecter
    val ftp = proxyKeys("ftp") map mapper collect collecter
    val all = http ::: https ::: ftp
  }

}
