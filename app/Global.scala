import play.api._
import play.api.mvc._


object Global extends GlobalSettings {

  override def onStart(app: Application) {
 }

  override def onStop(app: Application) {
    if (app.mode == Mode.Dev) {
      Logger.warn("Stopping started kernels")
      notebook.KernelManager.stopAll
    }
  }
}