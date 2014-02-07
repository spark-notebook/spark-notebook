/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook

import akka.testkit.TestKit
import kernel.remote.{RemoteActorSystem, SingleVM, AkkaConfigUtils}, SingleVM._
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import akka.actor._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout

class SingleVMSecurityTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  import _system.dispatcher
  def this() = this(ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load("subprocess-test"), "Cookie")))

  override def afterAll() {
    system.shutdown()
  }


  "A remote actor" should {

    implicit val timeout: Timeout = 10 seconds

    "inherit secure cookie" in {
      assert (AkkaConfigUtils.requiredCookie(system.settings.config).get === "Cookie")

      val remote = Await.result(RemoteActorSystem.spawn(_system, "kernel.conf"), 10 seconds)
      val echo = remote.actorOf(_system, Props(new EchoActor))

      echo ! "hello"
      expectMsg(3 seconds, "hello")
      echo ! PoisonPill

      remote.shutdownRemote()
    }

    "not allow unauthorized connections" ignore {

      val remote = Await.result(RemoteActorSystem.spawn(_system, "kernel.conf"), 10 seconds)
      val echo = remote.actorOf(_system, Props(new EchoActor))

      val unAuthSystem = ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load(), "Wrong Cookie"))
      val unAuthSender = new TestKit(unAuthSystem)

      echo.tell("hello", unAuthSender.testActor)
      assert (unAuthSender.receiveOne(3 seconds) === null)

      remote.shutdownRemote()

    }
  }
}
