import play.api._
import play.api.mvc.{Handler, RequestHeader}


object Global extends GlobalSettings {
  override def onStart(app: Application) {
  }

  override def onStop(app: Application) {
    if (app.mode == Mode.Dev) {
      Logger.warn("Stopping started kernels")
      notebook.KernelManager.stopAll
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