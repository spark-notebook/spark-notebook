/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.testkit.TestKit
import kernel.remote.{SingleVM, AkkaConfigUtils}, SingleVM._
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import akka.actor._
import akka.util.duration._
import com.typesafe.config.ConfigFactory
import akka.dispatch.{Await, Future}
import akka.pattern.ask
import akka.util.Timeout

class SingleVMSecurityTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load("subprocess-test"), "Cookie")))

  override def afterAll() {
    system.shutdown()
  }


  "A remote actor" must {

    implicit val timeout: Timeout = 5 seconds
    def spawn(vm: ActorRef, creator: => Actor): Future[ActorRef] = (vm ? Spawn(Props(creator))) map { _.asInstanceOf[ActorRef] }

    "inherit secure cookie" in {
      assert (AkkaConfigUtils.requiredCookie(system.settings.config).get === "Cookie")

      val vm = system.actorOf(Props[SingleVM])
      val echo = Await.result(spawn(vm, new EchoActor), 5 seconds)

      echo ! "hello"
      expectMsg(3 seconds, "hello")
      echo ! PoisonPill

      system.stop(vm)
    }

    "not allow unauthorized connections" in {
      val vm = system.actorOf(Props[SingleVM])
      val echo = Await.result(spawn(vm, new EchoActor), 5 seconds)

      val unAuthSystem = ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load(), "Wrong Cookie"))
      val unAuthSender = new TestKit(unAuthSystem)

      echo.tell("hello", unAuthSender.testActor)
      assert (unAuthSender.receiveOne(3 seconds) === null)

      system.stop(vm)
    }
  }

}

