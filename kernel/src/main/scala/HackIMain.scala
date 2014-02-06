package scala.tools.nsc
package interpreter

/**
 * Subclass to access some hidden things I need and also some custom behavior.
 */
class HackIMain(settings: Settings, out: JPrintWriter) extends IMain(settings, out) {
  def previousRequests = prevRequestList
}
