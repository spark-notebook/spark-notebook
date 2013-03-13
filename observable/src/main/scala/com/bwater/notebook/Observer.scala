/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

/**
 * Author: Ken
 */
trait Observer[T] extends rx.Observer[T] {
  def map[A](fxn: A=>T): Observer[A] =  new MappingObserver[T,A]{ def innerObserver = Observer.this; def observerMapper = fxn }
}

/**
 * A no-op observer, useful for extending just the methods you want
 * @tparam T
 */
trait ConcreteObserver[T] extends Observer[T] {
  def onCompleted() {}

  def onError(e: Exception) {}

  def onNext(args: T) {}
}

class NoopObserver[T] extends ConcreteObserver[T]

trait MappingObserver[A,B] extends Observer[B] {
  protected def innerObserver: Observer[A]

  protected def observerMapper: B=>A

  def onCompleted() {innerObserver.onCompleted()}

  def onError(e: Exception) {innerObserver.onError(e)}

  def onNext(args: B) {innerObserver.onNext(observerMapper(args))}
}

