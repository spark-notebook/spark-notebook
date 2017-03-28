package notebook.io

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object FutureUtil {

  // remove when scala.version >= 2.11
  def tryToFuture[T](t:Try[T]):Future[T] = t match {
    case Success(s) => Future.successful(s)
    case Failure(f) => Future.failed(f)
  }

  implicit class TryToFutureConverter[T](t: Try[T]) {
    def toFuture: Future[T] = {
      tryToFuture(t)
    }
  }
}
