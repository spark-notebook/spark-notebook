package scala.tools.nsc
package interpreter

/**
 * Subclass to access some hidden things I need and also some custom behavior.
 */
class HackIMain(settings: Settings, out: JPrintWriter) extends IMain(settings, out) {
  def previousRequests = prevRequestList

  override def createLineManager(): Line.Manager =
    new Line.Manager {
      // Users can write infinite loops that cannot be interrupted using
      // standard interrupt semantics.  We need a dramatic solution...
      override def onRunaway(line: Line[_]) =
        line.thread.stop()
    }
}