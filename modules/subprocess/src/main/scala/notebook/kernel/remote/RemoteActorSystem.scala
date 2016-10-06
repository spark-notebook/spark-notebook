package notebook.kernel.remote

import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.remote.RemoteScope
import com.typesafe.config.{Config, ConfigFactory}
import notebook.kernel.pfork.{BetterFork, ForkableProcess, ProcessInfo}
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Author: Ken
 */
class RemoteActorProcess extends ForkableProcess {
  // http://stackoverflow.com/questions/14995834/programmatically-obtain-ephemeral-port-with-akka
  private var _system: ActorSystem = null

  def init(args: Seq[String]): String = {
    val configFile = args.head
    val cfg = ConfigFactory.load(configFile)

    // Cookie file is optional second argument
    val actualCfg = args.take(2) match {
      case Seq(_, cookieFile) if cookieFile.size > 0 =>
        val cookie = FileUtils.readFileToString(new File(cookieFile))
        AkkaConfigUtils.requireCookie(cfg, cookie)
      case _ => cfg
    }

    _system = ActorSystem("Remote", actualCfg)

    val ws = _system.actorOf(Props[notebook.kernel.WebSocketAppender], "remote-logger")

    val address = GetAddress(_system).address
    address.toString
  }

  def waitForExit() {
    _system.awaitTermination()
    println("waitForExit complete")
  }
}

class FindAddressImpl(system: ExtendedActorSystem) extends Extension {
  def address = system.provider match {
    case rarp if rarp.getClass.getSimpleName == "RemoteActorRefProvider" => rarp.getDefaultAddress
    case _ => system.provider.rootPath.address
  }
}

object GetAddress extends ExtensionKey[FindAddressImpl]

case object RemoteShutdown

class ShutdownActor extends Actor {
  override def postStop() {
    // KV: I tried to do a context.system.shutdown() here, but the system would often hang when multiple actors were in play.
    //  I think it was this issue: https://groups.google.com/forum/#!msg/akka-user/VmKMPI_tNQU/ZUSz25OBpIwJ
    // So we take the hard way out. Would be nice to have graceful shutdown
    sys.exit(0)
  }

  def receive = Map.empty
}

/**
 * Represents a running remote actor system, with an address and the ability to kill it
 */
class RemoteActorSystem(localSystem: ActorSystem, info: ProcessInfo, remoteContext: ActorRefFactory) {

  def this(localSystem: ActorSystem, info: ProcessInfo) = this(localSystem, info, localSystem)

  val address = AddressFromURIString(info.initReturn)

  def deploy = Deploy(scope = RemoteScope(address))

  // this guy can `sys.exit` the remote process
  // since we use the `deploy` object to deploy it and its `postStop`
  val shutdownActor = remoteContext.actorOf(Props(new ShutdownActor).withDeploy(deploy))

  def shutdownRemote() {
    shutdownActor ! PoisonPill
  }

}

/**
 * Create a remote actor system
 */
object RemoteActorSystem {
  val nextId = new AtomicInteger(1)

  def spawn(config: Config, system: ActorSystem, configFile: String, kernelId: String,
    notebookPath: Option[String], customArgs:Option[List[String]]): Future[RemoteActorSystem] = {
    val cookiePath = ""
    new BetterFork[RemoteActorProcess](config, system.dispatcher, customArgs)
      .execute(kernelId, notebookPath.getOrElse("no-path"), configFile, cookiePath)
      .map {
        new RemoteActorSystem(system, _)
      }
  }
}
