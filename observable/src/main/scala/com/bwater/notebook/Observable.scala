/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

/**
 * Author: Ken
 * An observer trait, which is unfortunately lacking in rx.Subscription
 */
trait Observable[T] {
  def subscribe(observer: Observer[T]): rx.Subscription

//  def subscribe(next: T=>Unit): rx.Subscription = subscribe(new ConcreteObserver[T]{ override def onNext(args:T) = next(args) })

  def map[A](fxn: T=>A):Observable[A] = new Observable[A] {
    def subscribe(observer: Observer[A]) = Observable.this.subscribe(observer map fxn)
  }
}

class WrappedObservable[T](inner: rx.Observable[T]) extends Observable[T] {
  def subscribe(observer: Observer[T]) = inner.subscribe(observer)
}

class ConcreteObservable[T] extends Observable[T]  {
  protected val observableHandler = rx.subjects.Subject.create[T]

  def subscribe(observer: Observer[T]) = observableHandler.subscribe(observer)
}

trait MappingObservable[A,B] extends Observable[B] {
  protected def innerObservable: Observable[A]

  protected def observableMapper: A=>B

  def subscribe(observer: Observer[B]) = innerObservable.subscribe(observer map observableMapper)
}


object Observable {
  def just[T](x: T): Observable[T] = new WrappedObservable[T](rx.Observable.just(x))
}