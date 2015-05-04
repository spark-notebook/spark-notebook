package notebook

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

import akka.actor._
import akka.actor.Deploy

import com.typesafe.config.Config

import kernel.remote.{RemoteActorSystem, RemoteActorProcess}

/**
 * A kernel is a remote VM with a set of sub-actors, each of which interacts with  local resources (for example, WebSockets).
 * The local resource must be fully initialized before we will let messages flow through to the remote actor. This is
 * accomplished by blocking on actor startup
 * to the remote (this is accomplished by blocking on startup waiting for
 */
class Kernel(config:Config, system: ActorSystem, kernelId:String, val notebookPath:Option[String]=None) {
  implicit val executor = system.dispatcher

  val router = system.actorOf(Props(new ExecutionManager))

  private val remoteDeployPromise = Promise[Deploy]

  def remoteDeployFuture = remoteDeployPromise.future

  case object ShutdownNow

  def shutdown() { router ! ShutdownNow  }

  class ExecutionManager extends Actor with ActorLogging {
    // These get filled in before we ever receive messages
    var remoteInfo: RemoteActorSystem = null

    override def preStart() {
      remoteInfo = Await.result(RemoteActorSystem.spawn(config, system, "kernel", kernelId, notebookPath), 1 minutes)
      remoteDeployPromise.success(remoteInfo.deploy)
    }

    override def postStop() {
      if (remoteInfo != null)
        remoteInfo.shutdownRemote()
    }

    def receive = {
      case ShutdownNow =>
        if (remoteInfo != null) {
          remoteInfo.shutdownRemote()
        }
    }
  }
}

object KernelManager {
  def shutdown() {
    kernels.values foreach { _.shutdown() }
  }

  val kernels = new ConcurrentHashMap[String, Kernel]().asScala

  def get(id: String) = kernels.get(id)
  def apply(id: String) = kernels(id)
  def atPath(path:String) = kernels.find{ case (id, k ) => k.notebookPath.exists(_ == path) }
  def add(id:String, kernel: Kernel) {
    kernels += id -> kernel
  }

  def remove(id:String) {
    kernels -= id
  }

  def stopAll = {
    kernels.values.foreach(_.shutdown())
    kernels -- kernels.keys
  }
}
