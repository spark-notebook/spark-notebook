package utils

import akka.actor._
import notebook.server._

object AppUtils {

  import play.api.Play.current

  lazy val baseConfig = current.configuration

  lazy val notebookConfig = NotebookConfig(baseConfig.getConfig("manager").get)
  lazy val notebookManager = new NotebookManager(notebookConfig.projectName, notebookConfig.notebooksDir)
  lazy val clustersConf = notebookConfig.config.getConfig("clusters").get
  lazy val nbServerConf = baseConfig.getConfig("notebook-server").get.underlying
  lazy val kernelSystem = ActorSystem(
    "NotebookServer",
    nbServerConf,
    play.api.Play.classloader // todo more detail, this resolves the Play classloader problems w/ remoting
  )

  object proxy {
    def proxyKeys(pre: String) = List(
      "_proxy",
      "_proxy_user",
      "_proxy_pass",
      ".proxyHost",
      ".proxyPort",
      ".proxyUser",
      ".proxyPassword",
      ".nonProxyHosts"
    ).map(e => pre + e)

    def mapper = (path:String) => path → current.configuration.getString(path)
    def collecter:PartialFunction[(String, Option[String]),(String, String)] = { case (x:String, Some(y:String)) => x → y }
    //Proxy
    val http = proxyKeys("http") map mapper collect collecter
    val https = proxyKeys("https") map mapper collect collecter
    val ftp = proxyKeys("ftp") map mapper collect collecter
    val all = http ::: https ::: ftp
  }

}
