/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import kernel.remote.{RemoteActorSystem, SingleVM}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.scalatest.Matchers
import scala.concurrent.duration._
import scala.concurrent._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global

class RemoteActor(state: Int) extends Actor {
  def receive = {
    case 0 => sys.exit(1)
    case -1 => sys.error("Death!")
    case x: Int =>
      context.system.log.debug("RemoteActor received " + x)
      sender ! x * state
  }
}

class SingleVMTests extends TestKit(ActorSystem("SingleVMTests", ConfigFactory.load("subprocess-test"))) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll() {
    system.shutdown()
  }

  "Child process actors" should {
    implicit val timeout: Timeout = 5 seconds

    def retryUntilReceive(tries: Int, every: Duration)(action: => Unit): Option[Any] = {
      for (_ <- 0 until tries) {
        action
        val x = receiveOne(every)
        if (x != null) return Some(x)
      }
      None
    }

    "support simple actor and shutdown remote process" in {
      val remote = Await.result(RemoteActorSystem.spawn(system, "kernel.conf"), 10 seconds)
      val tester = remote.actorOf(system, Props(new RemoteActor(5)))

      tester ! 10

      assert(receiveOne(1 second) === 50)

      remote.shutdownRemote()
    }

    "support actor restarts" ignore {
      val remote = Await.result(RemoteActorSystem.spawn(system, "kernel.conf"), 10 seconds)
      val tester = remote.actorOf(system, Props(new RemoteActor(5)))

      tester ! 10

      assert(receiveOne(1 second) === 50)

      tester ! -1

      assert(retryUntilReceive(2, 1 second) {
        tester ! 5
      } === Some(25))

      remote.shutdownRemote()
    }

    // TODO: Write a test where we detect the death and restart manually
    "restart after a process termination" ignore {
      val remote = Await.result(RemoteActorSystem.spawn(system, "kernel.conf"), 10 seconds)
      val tester = remote.actorOf(system, Props(new RemoteActor(5)))

      tester ! 10

      watch(tester)

      assert(receiveOne(1 second) === 50)

      tester ! 0

      assert(receiveOne(1 second) === 50)


      assert(retryUntilReceive(5, 1 second) {
        tester ! 5
      } === Some(25))

      remote.shutdownRemote()
    }

    "not affect each other" ignore { //Does not work in CI
      val vms = for {
        i <- 0 to 3
      } yield RemoteActorSystem.spawn(system, "kernel.conf")

      val vmNow =  Await.result(Future.sequence(vms), 20 seconds)
      val remotes = for {
        (vm, i) <- vmNow.zipWithIndex
        remote <- Seq(vm.actorOf(system, Props( new RemoteActor(5 + i))), vm.actorOf(system, Props( new RemoteActor(5 + i))))
      } yield remote


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

      vmNow.foreach(_.shutdownRemote())
    }
  }
}
