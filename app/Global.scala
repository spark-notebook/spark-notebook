import play.api._
import play.api.mvc._


object Global extends GlobalSettings {

  override def onStart(app: Application) {
    Logger.warn("Start local micro tachyon")
    notebook.share.Tachyon.start
 }

  override def onStop(app: Application) {
    Logger.warn("Stop local micro tachyon")
    notebook.share.Tachyon.stop
  }
}