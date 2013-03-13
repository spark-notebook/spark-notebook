/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor._

/**
 * An actor which is expecting to transition through several states guarded by one or more expected messages, and
 * which will stash any unexpected messages to be replayed at each state transition.
 */
abstract class GuardedActor extends Actor with Stash {

  type &[+A, +B] = (A, B)
  object & {
    def unapply[A, B](tuple: A & B) = Some(tuple)
  }

  private sealed trait GuardResult[+A] {
    def transformResult[B](ifTransition: Guard[A] => Guard[B], ifComplete: A => GuardResult[B]): GuardResult[B] = this match {
      case Pass => Pass
      case Continue(next) => Continue(ifTransition(next))
      case Restart(next) => Restart(ifTransition(next))
      case Complete(value) => ifComplete(value)
    }
    def orElse[B >: A](next: => GuardResult[B]): GuardResult[B] = if (this == Pass) next else this
  }
  private case object Pass extends GuardResult[Nothing]
  private final case class Continue[A](next: Guard[A]) extends GuardResult[A]
  private final case class Restart[A](next: Guard[A]) extends GuardResult[A]
  private final case class Complete[A](value: A) extends GuardResult[A]

  trait Guard[+A] { self =>

    protected[GuardedActor] def >>:(msg: Any): GuardResult[A]

    def &[B](other: Guard[B]): Guard[A & B] = new Guard[A & B] {
      private class Partial[X](remaining: Guard[X])(result: X => A & B) extends Guard[A & B] {
        protected[GuardedActor] def >>:(msg: Any) = msg >>: remaining transformResult (new Partial(_)(result), result andThen Complete.apply)
      }

      protected[GuardedActor] def >>:(msg: Any) =
        (msg >>: self  transformResult (_ & other, a => Continue(new Partial(other)((a, _))))) orElse
        (msg >>: other transformResult (self & _ , b => Continue(new Partial(self) ((_, b)))))
    }

    def filter(f: A => Boolean): Guard[A] = new Guard[A] {
      protected[GuardedActor] def >>:(msg: Any) = msg >>: self transformResult (_ filter f, a => if (f(a)) Complete(a) else Pass)
    }

    def map[B](f: A => B): Guard[B] = new Guard[B] {
      protected[GuardedActor] def >>:(msg: Any) = msg >>: self transformResult (_ map f, f andThen Complete.apply)
    }

    def flatMap[B](f: A => Guard[B]): Guard[B] = new Guard[B] {
      protected[GuardedActor] def >>:(msg: Any) = msg >>: self transformResult (_ flatMap f, f andThen Restart.apply)
    }
  }

  def get[A](receive: PartialFunction[Any, A]): Guard[A] = new Guard[A] {
    protected[GuardedActor] def >>:(msg: Any) = receive.lift(msg) match {
      case None => Pass
      case Some(x) => Complete(x)
    }
  }

  def getType[A : Manifest]: Guard[A] = new Guard[A] {
    protected[GuardedActor] def >>:(msg: Any) = if (manifest[A].erasure.isInstance(msg)) Complete(msg.asInstanceOf[A]) else Pass
  }

  private[this] def receiveGuarded(guard: Guard[Unit]): Receive = {
    case msg => msg >>: guard match {
      case Pass =>
        stash()
      case Continue(next) =>
        context.become(receiveGuarded(next))
      case Restart(next) =>
        unstashAll()
        context.become(receiveGuarded(next))
      case Complete(()) =>
        unstashAll()
    }
  }

  def receive = receiveGuarded(guard.map(context.become(_)))

  def guard: Guard[Receive]
}