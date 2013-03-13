/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

/**
 * Author: Ken
 * An observer and observable -  mutable data that can be changed or subscribed to
 */
trait Connection[T] {

  def observable: Observable[T]
  def observer: Observer[T]

  def biMap[A](codec: Codec[T,A]): Connection[A] = new MappingConnection[T,A](Connection.this, codec)

  def biMap[A](aToB: A=>T, bToA: T=>A): Connection[A] = biMap[A](new Codec[T,A] {
    def encode(x: T) = bToA(x)
    def decode(x: A) = aToB(x)
  })

  def <--(other: Connection[T]) {
    other.observable.subscribe(observer)
  }

  def -->(other: Connection[T]) {
    observable.subscribe(other.observer)
  }

  def <-->(other: Connection[T]) {
    this <-- other
    this --> other
  }
}

class ConcreteConnection[T](val observable: Observable[T], val observer: Observer[T]) extends Connection[T]

object Connection {
  def just[T](v: T) = new ConcreteConnection[T](Observable.just(v), new NoopObserver[T]())
}

class MappingConnection[A,B](inner:Connection[A], codec: Codec[A,B]) extends Connection[B] {
  val observable =  new MappingObservable[A,B] {
    protected def innerObservable = inner.observable
    protected def observableMapper = codec.encode
  }

  val observer = new MappingObserver[A,B] {
    protected def innerObserver = inner.observer
    protected def observerMapper = codec.decode
  }
}