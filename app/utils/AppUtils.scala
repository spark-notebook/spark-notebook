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
}