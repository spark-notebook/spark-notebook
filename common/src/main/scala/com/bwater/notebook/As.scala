/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import scalaz._, Scalaz._

case class As[P[_], A](value: P[A]) extends NewType[P[A]]

object As {
  implicit def pureToAs[P[_], A](a: A)(implicit P: Pure[P]): As[P, A] = As[P, A](P.pure(a))
  implicit def identityToAs[P[_], A](a: P[A]): As[P, A] = As[P, A](a)
}