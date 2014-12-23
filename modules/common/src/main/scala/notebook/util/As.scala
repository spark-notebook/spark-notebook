package notebook.util

import scalaz._, Scalaz._

case class As[P[_], A](self: P[A]) extends syntax.Ops[P[A]]

object As {
  implicit def pureToAs[P[_], A](a: A)(implicit P: Applicative[P]): As[P, A] = As[P, A](P.pure(a))
  implicit def identityToAs[P[_], A](a: P[A]): As[P, A] = As[P, A](a)
}
