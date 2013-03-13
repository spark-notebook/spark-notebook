/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import kernel.remote.SingleVM
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import org.scalatest.matchers.MustMatchers
import akka.util.duration._
import akka.dispatch.{Future, Await}
import akka.util.{Duration, Timeout}
import com.typesafe.config.ConfigFactory

class RemoteActor(state: Int) extends Actor {
  def receive = {
    case 0 => sys.exit(1)
    case -1 => sys.error("Death!")
    case x: Int =>
      context.system.log.debug("RemoteActor received " + x)
      sender ! x * state
  }
}

class SingleVMTests extends TestKit(ActorSystem("SingleVMTests", ConfigFactory.load("subprocess-test"))) with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll {
  
  import SingleVM._

  override def afterAll() {
    system.shutdown()
  }

  "Child process actors" must {

    import akka.pattern.ask
    implicit val timeout: Timeout = 5 seconds
    
    def spawn(vm: ActorRef, creator: => Actor): Future[ActorRef] = (vm ? Spawn(Props(creator))) map { _.asInstanceOf[ActorRef] }

    def retryUntilReceive(tries: Int, every: Duration)(action: => Unit): Option[Any] = {
      for (_ <- 0 until tries) {
        action
        val x = receiveOne(every)
        if (x != null) return Some(x)
      }
      None
    }

    "restart after an actor death" in {
      val vm = system.actorOf(Props[SingleVM])

      val remote = Await.result(spawn(vm, new RemoteActor(5)), 5 seconds)

      remote ! 10

      assert(receiveOne(1 second) === 50)

      remote ! -1

      assert(retryUntilReceive(2, 1 second) {
        remote ! 5
      } === Some(25))

      system.stop(vm)
    }

    "restart after a process termination" in {
      val vm = system.actorOf(Props[SingleVM])

      val remote = Await.result(spawn(vm, new RemoteActor(5)), 5 seconds)

      remote ! 10

      assert(receiveOne(1 second) === 50)

      remote ! 0

      assert(retryUntilReceive(5, 1 second) {
        remote ! 5
      } === Some(25))

      system.stop(vm)
    }

    "not affect each other" ignore { //Does not work in CI
      val vms = for (i <- 0 to 3) yield system.actorOf(Props[SingleVM])
      val remotes = Await.result(Future.sequence(vms.zipWithIndex flatMap { case (vm, i) => Seq(spawn(vm, new RemoteActor(5 + i)), spawn(vm, new RemoteActor(10 + i))) }), 5 seconds)

      for (remote <- remotes) {
        remote ! 10
      }

      assert(receiveN(8).toSet === Set(50, 60, 70, 80, 100, 110, 120, 130))

      remotes.head ! -1

      for (remote <- remotes.tail) {
        remote ! 5
      }

      assert(receiveN(7).toSet === Set(30, 35, 40, 50, 55, 60, 65))

      assert(retryUntilReceive(5, 1 second) {
        remotes.head ! 5
      } === Some(25))

      vms.foreach(system.stop)
    }
  }
}