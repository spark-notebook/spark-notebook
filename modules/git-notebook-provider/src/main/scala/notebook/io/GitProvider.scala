package notebook.io

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path}

import com.jcraft.jsch.Session
import com.typesafe.config.Config
import org.eclipse.jgit.api.{CloneCommand, Git, TransportCommand, TransportConfigCallback}
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.OpenSshConfig.Host
import org.eclipse.jgit.transport.{JschConfigSessionFactory, SshTransport, Transport, UsernamePasswordCredentialsProvider}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}
import notebook.io.FutureUtil._

trait GitProvider {
  def init()(implicit ec:ExecutionContext): Future[Unit]
  def refreshLocal()(implicit ec:ExecutionContext): Future[Unit]
  def add(relativePath: String, commitMessage: String)(implicit ec:ExecutionContext): Future[Unit]
  def push()(implicit ec:ExecutionContext) : Future[Unit]
  def remove(relativePath: String, commitMessage: String)(implicit ec:ExecutionContext): Future[Unit]
  def checkout(relativePath: String, commit: String)(implicit ec:ExecutionContext): Future[Unit]
  def versions(relativePath: String)(implicit ec:ExecutionContext): Future[List[Version]]

  def gitContext: GitContext
}

trait GitContext {
  def git: Git
  def branch: String
  def commitMessages: CommitMessages
  def localPath: Path

  // some shortcuts for the GIT Porcelain API
  def rm = git.rm()
  def commit = git.commit()
  def add = git.add()
  def checkout = git.checkout()
  def log = git.log()
}

object GitContext {
  object implicits {
    implicit class CloneCommandOps(val cmd : CloneCommand) extends AnyRef {
      def setURI(uri:URI) = cmd.setURI(uri.toString)
    }
  }
}

trait RemoteGitContext extends GitContext {
  def auth: Auth
  def remote: URI

  // some shortcuts for the GIT Porcelain API
  def pull = git.pull()
  def push = git.push()
}

trait LocalGitProvider extends GitProvider { gitCtx : GitContext =>

  override def refreshLocal()(implicit ec:ExecutionContext): Future[Unit] = Future.successful()

  override def add(relativePath: String, commitMessage: String)(implicit ec:ExecutionContext): Future[Unit] = {
    Future {
      gitCtx.add.addFilepattern(relativePath).call()
      gitCtx.commit.setMessage(commitMessage).call()
    }
  }

  override def push()(implicit ec:ExecutionContext): Future[Unit] = Future.successful()

  override def remove(relativePath: String, commitMessage: String)(implicit ec:ExecutionContext): Future[Unit] = {
    Future {
      gitCtx.rm.addFilepattern(relativePath).call()
      gitCtx.commit.setMessage(commitMessage).call()
    }
  }

  override def checkout(relativePath: String, commit: String)(implicit ec:ExecutionContext): Future[Unit] = {
    Future{
      gitCtx.checkout.addPath(relativePath).setForce(true).setStartPoint(commit).call()
    }
  }

  override def versions(relativePath: String)(implicit ec:ExecutionContext): Future[List[Version]] = Future {
    import scala.collection.JavaConverters._
    val logs = gitCtx.log.addPath(relativePath).call()
    logs.iterator().asScala.toList.map(commitRef => Version(commitRef.name(), commitRef.getFullMessage, commitRef.getCommitTime))
  }

}

trait RemoteGitProvider extends LocalGitProvider { gitCtx: RemoteGitContext =>

  override def refreshLocal()(implicit ec:ExecutionContext): Future[Unit] = Future {
    gitCtx.auth(gitCtx.pull).call()
  }

  override def push()(implicit ec:ExecutionContext):Future[Unit] = Future {
    // FIXME: ideally we shouldn't need to force the pushUrl
    gitCtx.auth(gitCtx.push.setRemote(gitCtx.remote.toString)).call()
  }

}

sealed trait Auth {
  def apply[T1 <: TransportCommand[T1, _], _](cmd: T1 ): T1
}

class KeyFileAuth(keyFile: String, passphrase: Option[String]) extends Auth {
  def apply[T1 <: TransportCommand[T1, _], _](cmd: T1): T1 = {
    val keyFileTransportConfigCallback = new TransportConfigCallback {
      override def configure(transport: Transport): Unit = {
        val t = transport.asInstanceOf[SshTransport]
        t.setSshSessionFactory(new JschConfigSessionFactory {

          import org.eclipse.jgit.util.FS
          import com.jcraft.jsch.JSch

          override protected def createDefaultJSch(fs: FS): JSch = {
            val default = super.createDefaultJSch(fs)
            default.removeAllIdentity()
            passphrase match {
              case Some(pass) => default.addIdentity(keyFile, pass)
              case None => default.addIdentity(keyFile)
            }
            default
          }

          override def configure(hc: Host, session: Session): Unit = {
            session.setConfig("StrictHostKeyChecking", "no")
          }
        })
      }
    }
    cmd.setTransportConfigCallback(keyFileTransportConfigCallback)
  }
}

class PasswordAuth(password:String) extends Auth {
  def apply[T1 <: TransportCommand[T1, _], _](cmd: T1): T1 = {
    val passwordTransportConfigCallback = new TransportConfigCallback {
      override def configure(transport: Transport): Unit = {
        val t = transport.asInstanceOf[SshTransport]
        t.setSshSessionFactory(new JschConfigSessionFactory {
          override def configure(hc: Host, session: Session): Unit = {
            session.setPassword(password)
          }
        })
      }
    }
    cmd.setTransportConfigCallback(passwordTransportConfigCallback)
  }
}

class UsernamePasswordAuth(username: String, password: String) extends Auth {
  override def apply[T1 <: TransportCommand[T1, _], _](cmd: T1): T1 = {
    val cred = new UsernamePasswordCredentialsProvider(username, password)
    cmd.setCredentialsProvider(cred)
    cmd
  }
}

case class CommitMessages(initialCommit: String, delete: String, commit: String, move: String)

object CommitMessagesConfiguration extends Configurable[CommitMessages] {
  import ConfigUtils._

  val DefaultInitialCommit: String = "Initial commit"
  val DefaultDeleteMessage: String = "Removed file"
  val DefaultCommitMessage: String = "Saved file"
  val DefaultMoveMessage: String = "Moved file"

  def apply(config: Config)(implicit ec:ExecutionContext): Future[CommitMessages] = {
    val initialCommitMessage = config.getSafeString("messages.initial_commit").getOrElse(DefaultInitialCommit)
    val commitMessageSave = config.getSafeString("messages.save").getOrElse(DefaultCommitMessage)
    val commitMessageRemove = config.getSafeString("messages.remove").getOrElse(DefaultDeleteMessage)
    val commitMessagesMove = config.getSafeString("messages.move").getOrElse(DefaultMoveMessage)
    Future(CommitMessages(initialCommitMessage, commitMessageRemove, commitMessageSave, commitMessagesMove ))
  }

}

case class LocalRepo(
                      pathState: PathState,
                      override val branch:String,
                      override val commitMessages: CommitMessages
                    ) extends GitContext with LocalGitProvider {
  val repository: Repository = new FileRepositoryBuilder().setWorkTree(pathState.toFile).build()
  override lazy val git = new Git(repository)
  override val localPath: Path = pathState.path
  override def gitContext: GitContext = this

  def init()(implicit ec:ExecutionContext): Future[Unit] = Future {
    pathState match {
      case ep: EmptyPath => // Init git in there
        Git.init().setBare(false).setDirectory(ep.toFile).call()
        if (branch != GitNotebookProvider.DefaultBranch) {
          checkout.setCreateBranch(true).setName(branch).call()
        }

      // This case might not work (creating a branch that might already exist)
      case nep: NonEmptyGitPath => // Use existing local GIT repo
        //Git.init().setBare(false).setDirectory(_localPathState.toFile).call() ??
        if (branch != GitNotebookProvider.DefaultBranch) {
          checkout.setCreateBranch(true).setName(branch).call()
        }

      case negp: NonEmptyNonGitPath => // existing non-git path, init git repo and add everything in there
        Git.init().setBare(false).setDirectory(negp.toFile).call()
        val repoLocation = negp.toFile.toString
        add.addFilepattern(repoLocation).call()
        commit.setMessage(s"${commitMessages.initialCommit} $repoLocation").call()
    }
  }

  // path location of this repo
  def path: Path = pathState.path

}

object LocalRepoConfiguration {
  import ConfigUtils._

  val LocalPathKey = "local_path"

  def parse(config:Config, commitMessages: CommitMessages) : Try[LocalRepo] = {
    val branch = config.getSafeString("branch").getOrElse(GitNotebookProvider.DefaultBranch)
    config.tryGetString(LocalPathKey).flatMap(str => PathState(str)).map(ps => LocalRepo(ps, branch, commitMessages))
  }
}

case class RemoteRepo(
                       localRepo: LocalRepo,
                       override val remote: URI,
                       override val auth: Auth
                     ) extends RemoteGitContext with RemoteGitProvider {
  override val git = localRepo.git
  override val branch = localRepo.branch
  override val commitMessages = localRepo.commitMessages
  override val localPath: Path = localRepo.pathState.path
  override def gitContext: GitContext = this
  import GitContext.implicits._
  import scala.concurrent.ExecutionContext.Implicits.global

  override def init()(implicit ec:ExecutionContext): Future[Unit] =  localRepo.pathState match {
    // Empty local dir => clone remote
    case ep: EmptyPath => cloneToLocal()

    case negp: NonEmptyGitPath => // existing path is assumed to be a clone of the remote.
      Future{checkout.setName(branch).call(); ()}

    case negp: NonEmptyNonGitPath => // we don't support an existing non-git directory. Set aside and re-recreate
      moveCurrentPath()
      Future{PathState.create(localRepo.path)}.flatMap(_ => cloneToLocal())
  }

  private[io] def moveCurrentPath() : Unit = {
    val currentPath = localRepo.path
    val dirname = currentPath.getFileName
    val targetDirName = s"$dirname.moved-${System.currentTimeMillis()}-${Random.nextInt(1000)}"
    val targetMovePath = currentPath.resolveSibling(targetDirName)
    Files.move(currentPath, targetMovePath)
  }

  private[io] def cloneToLocal(): Future[Unit] = Future {
    val cmd = Git.cloneRepository().setCloneAllBranches(true)
    cmd.setDirectory(localRepo.path.toFile)
    cmd.setURI(remote)
    println(s"Cloning remote git repo '$remote' into '${localRepo.path.toFile}'; and switching branch to '$branch'")
    auth(cmd).call()
    checkout.setName(branch).call()
    ()
  }
}

object RemoteRepoConfiguration {

  import ConfigUtils._

  def toURI(s : String) : Try[URI] = Try(new URI(s))
  def checkFileExists(file : String) : Try[String] = if (new File(file).exists() ) {
    Success(file)
  } else {
    Failure(new ConfigurationCorruptException(s"Key file $file does not exist."))
  }

  def validateRepoUrl(repo: URI): Try[URI] = {
    val SupportedProtocols = Set ("http", "https", "ssh")
    if (SupportedProtocols( repo.getScheme )) {
      Success(repo)
    } else {
      Failure(new ConfigurationCorruptException(s"Git repository protocol is not supported: ${repo.getScheme}"))
    }
  }

  def parseRemote(repo: String): Try[URI] = {
    val githubSshUriPrefix = "git@github.com:"
    repo match {
      case _ if repo.startsWith(githubSshUriPrefix) =>
        parseRemote(repo.replace(githubSshUriPrefix, "ssh://git@github.com/"))

      case _ => toURI(repo).flatMap(validateRepoUrl _)
    }
  }

  def parse(config:Config): Try[Option[LocalRepo => RemoteRepo]] = {

    def mkRepo(location: URI, auth: Auth): Try[Option[LocalRepo => RemoteRepo]] = Success(Some(localRepo => RemoteRepo(localRepo, location, auth)))

    val remoteURI = config.getSafeString("remote").map(parseRemote)

    val authKey = config.getSafeString("authentication.key_file").map(checkFileExists _ )
    val passphrase = config.getSafeString("authentication.key_file_passphrase")
    val username = config.getSafeString("authentication.username")
    val password = config.getSafeString("authentication.password")


    (remoteURI, authKey, username, password) match {
      case (None, _, _, _) => Success(None)
      case (Some(Failure(ex)), _, _,_) => Failure(ex)
      case (Some(Success(location)), Some(Failure(filex)), _, _) => Failure(filex)
      case (Some(Success(location)), Some(Success(keyfile)), _, _) => mkRepo(location, new KeyFileAuth(keyfile, passphrase))
      case (Some(Success(location)), None, Some(user), Some(pwd)) => mkRepo(location, new UsernamePasswordAuth(user, pwd))
      case (Some(Success(location)), None, None, Some(pwd)) => mkRepo(location, new PasswordAuth(pwd))
      case _ =>  Failure(new ConfigurationCorruptException("Invalid Remote Git Repository Configuration"))
    }
  }
}

class GitProviderConfigurator extends Configurable[GitProvider] {

  override def apply(config: Config)(implicit ec:ExecutionContext): Future[GitProvider] = {

    val repoConfig = for {
      commitMessages <- CommitMessagesConfiguration(config)
      local <- tryToFuture(LocalRepoConfiguration.parse(config, commitMessages))
      remote <- tryToFuture(RemoteRepoConfiguration.parse(config))
    } yield (local, remote)


    repoConfig.flatMap { case (localRepo, remoteRepoFunc) =>
      val gitProvider: GitProvider = remoteRepoFunc.map { localToRemote =>
        localToRemote(localRepo)
      }.getOrElse {
        localRepo
      }
      gitProvider.init().map(_ => gitProvider)
    }
  }

}
