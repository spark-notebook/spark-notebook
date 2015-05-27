import play.api._


object Global extends GlobalSettings {

  override def onStart(app: Application) {
    app.configuration.getString("manager.tachyon.url") match {
      case None =>
        Logger.warn("Start local micro tachyon")
        notebook.share.Tachyon.start
      case Some(x) =>
        Logger.info("Using tachyon at " + x)
    }
  }

  override def onStop(app: Application) {
    if (app.mode == Mode.Dev) {
      Logger.warn("Stopping started kernels")
      notebook.KernelManager.stopAll
    }

    app.configuration.getString("manager.tachyon.url") match {
      case None =>
        Logger.warn("Stop local micro tachyon")
        notebook.share.Tachyon.stop
      case Some(x) =>
        Logger.info("Still using tachyon at " + x)
    }
  }
}