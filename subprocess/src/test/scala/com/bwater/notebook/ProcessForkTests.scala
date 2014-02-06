package com.bwater.notebook

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{Actor, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import kernel.pfork.{BetterFork, ForkableProcess, ProcessFork}
import kernel.remote.{RemoteActorSystem,  AkkaConfigUtils}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.scalatest.Matchers
import scala.concurrent._
import scala.concurrent.duration._
import java.lang.Exception
import scala.Exception
import org.apache.commons.exec.ExecuteException
import akka.remote.RemoteScope
import akka.pattern.AskSupport
import akka.util.Timeout

/**
 * Author: Ken
 */
class SimpleProcess extends ForkableProcess {
  def init(args: Seq[String]) = "hello"
  def waitForExit() { Thread.sleep(200) }
}

class CrasherProcess extends ForkableProcess {
  def init(args: Seq[String]) = sys.exit(2)
  def waitForExit() { Thread.sleep(200) }
}

class ProcessForkTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with AskSupport {
  implicit val timeout:Timeout = 10 seconds
  def this() = this(ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load("subprocess-test"), "Cookie")))

  "ProcesFork" should {
    "Spawn a simple process" in {
      val fork = new BetterFork[SimpleProcess](_system.dispatcher)
      val resp = Await.result(fork.execute(), 5 seconds)
      resp.initReturn should equal("hello")
    }


    "Handle remote crashing on initialize" in {
      val fork = new BetterFork[CrasherProcess](_system.dispatcher)
      evaluating { Await.result(fork.execute(), 5 seconds) } should produce [ExecuteException]
    }
  }

  "RemoteActorSystem" should {
    "Create a simple actor" in {
      val remote = Await.result(RemoteActorSystem.spawn(_system, "subprocess-test"), 5 seconds)

      val actor = remote.actorOf(_system, Props(new Actor{
        def receive = {
          case 1 => sender ! 2
        }
      }))

      Await.result((actor ? 1).mapTo[Int], 5 seconds) should equal(2)
      remote.shutdownRemote()
    }
  }
}
