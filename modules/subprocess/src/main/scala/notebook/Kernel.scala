package notebook

import java.util.concurrent.ConcurrentHashMap

import akka.actor.{Deploy, _}
import com.typesafe.config.Config
import notebook.kernel.remote.RemoteActorSystem

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * A kernel is a remote VM with a set of sub-actors, each of which interacts with  local resources (for example, WebSockets).
 * The local resource must be fully initialized before we will let messages flow through to the remote actor. This is
 * accomplished by blocking on actor startup
 * to the remote (this is accomplished by blocking on startup waiting for
 */
class Kernel(
  config: Config,
  system: ActorSystem,
  kernelId: String,
  notebookPath_ : Option[String] = None,
  customArgs:Option[List[String]]
) {
  private[this] var _notebookPath = notebookPath_

  def notebookPath = _notebookPath

  def moveNotebook(to:String) {
    _notebookPath = Some(to)
  }

  implicit val executor = system.dispatcher

  val router = system.actorOf(Props(new ExecutionManager))

  private val remoteDeployPromise = Promise[Deploy]()

  def remoteDeployFuture = remoteDeployPromise.future

  case object ShutdownNow

  def shutdown() {
    router ! ShutdownNow
  }

  class ExecutionManager extends Actor with ActorLogging {
    // These get filled in before we ever receive messages
    private var remoteInfo: RemoteActorSystem = null

    override def preStart() {
      remoteInfo = Await.result(RemoteActorSystem.spawn(config, system, "kernel", kernelId, notebookPath, customArgs), 1 minutes)
      remoteDeployPromise.success(remoteInfo.deploy)
    }

    def shutdownRemote() = {
      Option(remoteInfo).foreach(_.shutdownRemote())
      KernelManager.remove(kernelId)
    }

    override def postStop() {
      shutdownRemote()
    }

    def receive = {
      case ShutdownNow =>
        shutdownRemote()
    }
  }

}

object KernelManager {
  def shutdown() {
    kernels.values foreach {
      _.shutdown()
    }
  }

  private val kernels = new ConcurrentHashMap[String, Kernel]().asScala

  def get(id: String) = kernels.get(id)

  def apply(id: String) = kernels(id)

  def atPath(path: String) = kernels.find { case (id, k) => k.notebookPath.exists(_ == path) }

  def add(id: String, kernel: Kernel) {
    kernels += id -> kernel
  }

  def remove(id: String) {
    kernels -= id
  }

  def stopAll = {
    kernels.values.foreach(_.shutdown())
    kernels -- kernels.keys
  }
}
