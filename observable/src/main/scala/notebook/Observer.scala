/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package notebook

import rx.lang.scala.{Observable => RxObservable, Observer => RxObserver, _}

/**
 * Author: Ken
 */
trait Observer[T] extends RxObserver[T] {
  def map[A](fxn: A=>T): Observer[A] =  new MappingObserver[T,A]{
    def innerObserver = Observer.this;
    def observerMapper = fxn
  }
}

/**
 * A no-op observer, useful for extending just the methods you want
 * @tparam T
 */
trait ConcreteObserver[T] extends Observer[T] {
  override def onCompleted() {}

  def onError(e: Exception) {}

  override def onNext(args: T) {}
}

class NoopObserver[T] extends ConcreteObserver[T]

trait MappingObserver[A,B] extends Observer[B] {
  protected def innerObserver: Observer[A]

  protected def observerMapper: B=>A

  override def onCompleted() {innerObserver.onCompleted()}

  def onError(e: Exception) {innerObserver.onError(e)}

  override def onNext(args: B) {innerObserver.onNext(observerMapper(args))}
}

