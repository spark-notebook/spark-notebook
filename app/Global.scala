import play.api._
import play.api.mvc.{Handler, RequestHeader}


object Global extends GlobalSettings {

  override def onStart(app: Application) {
    if (app.configuration.getBoolean("manager.tachyon.enabled").getOrElse(false)) {
      app.configuration.getString("manager.tachyon.url") match {
        case None =>
          Logger.warn("Start local micro tachyon")
          notebook.share.Tachyon.start
        case Some(x) =>
          Logger.info("Using tachyon at " + x)
      }
    }
  }

  override def onStop(app: Application) {
    if (app.mode == Mode.Dev) {
      Logger.warn("Stopping started kernels")
      notebook.KernelManager.stopAll
    }

    if (app.configuration.getBoolean("manager.tachyon.enabled").getOrElse(false)) {
      app.configuration.getString("manager.tachyon.url") match {
        case None =>
          Logger.warn("Stop local micro tachyon")
          notebook.share.Tachyon.stop
        case Some(x) =>
          Logger.info("Still using tachyon at " + x)
      }
    }
  }

  // http://stackoverflow.com/a/26877713/56250
  def removeTrailingSlash(origReq: RequestHeader): RequestHeader = {
    if (origReq.path.endsWith("/") && origReq.path != "/") {
      val path = origReq.path.stripSuffix("/")
      if (origReq.rawQueryString.isEmpty)
        origReq.copy(path = path, uri = path)
      else
        origReq.copy(path = path, uri = path + s"?${origReq.rawQueryString}")
    } else {
      origReq
    }
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] =
    super.onRouteRequest(removeTrailingSlash(request))

}