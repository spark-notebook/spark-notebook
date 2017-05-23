package notebook.io


import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import notebook.{Notebook, NotebookNotFoundException, Resource}

object GitNotebookProvider {
  val DefaultBranch = "master"
  case class CommitSpec(message: String)
}

class GitNotebookProviderConfigurator extends Configurable[NotebookProvider] {

  import ConfigUtils._
  import scala.concurrent.duration._
  val InitializationTimeout = 10 seconds

  override def apply(config: Config)(implicit ec:ExecutionContext): Future[NotebookProvider] = {
    for {
      commitMessages <- CommitMessagesConfiguration(config)
      gitProvider <- (new GitProviderConfigurator())(config)
    } yield new ConfigurableGitNotebookProvider(gitProvider, commitMessages)
  }
}

class ConfigurableGitNotebookProvider(val gitProvider: GitProvider, commitMsgs: CommitMessages) extends NotebookProvider {
  import GitNotebookProvider._

  override def isVersioningSupported: Boolean = true

  override val root = gitProvider.gitContext.localPath
  def relativize(path:Path) = root.relativize(path)

  // refresh dir => fs.list
  override def list(path: Path)(implicit ev: ExecutionContext): Future[List[Resource]] = {
    gitProvider.refreshLocal().flatMap(_ => super.list(relativize(path)))
  }

  // refresh dir => fs.get file
  override def get(path: Path, version: Option[Version] = None)(implicit ev: ExecutionContext): Future[Notebook] = {
    val checkoutVersion = version.map{version =>
      gitProvider.checkout(relativize(path).toString, version.id)
    }.getOrElse{ Future.successful(())}
    for {
      _ <- gitProvider.refreshLocal()
      _ <- checkoutVersion
      notebook <- if (Files.exists(path)) {
        Notebook.deserializeFuture(new String(Files.readAllBytes(path), StandardCharsets.UTF_8))
      } else {
        Future.failed(new NotebookNotFoundException(path.toString))
      }
    } yield notebook
  }

  // check exists => fs.delete => git.delete
  override def delete(path: Path)(implicit ev: ExecutionContext): Future[Notebook] = {
    val relativePathStr = relativize(path).toString
    for {
      notebook <- get(path)
      _ <- {
        val deleted = Files.deleteIfExists(path)
        if (deleted) {
          gitProvider.remove(relativePathStr, s"${commitMsgs.delete} $relativePathStr")
        } else {
          Future.failed(new NotebookNotFoundException(relativePathStr))
        }
      }
      _ <- gitProvider.push()
    } yield notebook
  }

  override def versions(path:Path)(implicit ev: ExecutionContext): Future[List[Version]] = {
    gitProvider.versions(relativize(path).toString)
  }

  // fs.save => git.commit => git.push
  override def save(path: Path, notebook: Notebook, saveSpec: Option[String]= None)(implicit ev: ExecutionContext): Future[Notebook] = {
    val relativePathStr = relativize(path).toString
    val commitMsg = saveSpec.getOrElse(s"${commitMsgs.commit} $relativePathStr")
    for {
      nb <- Notebook.serializeFuture(notebook)
      _ <- Future{Files.write(path, nb.getBytes(StandardCharsets.UTF_8))}
      _ <- gitProvider.add(relativePathStr, commitMsg)
      _ <- gitProvider.push()
      persistedNb <- get(path)
    } yield persistedNb
  }

  override def moveInternal(src: Path, dest: Path)(implicit ev: ExecutionContext): Future[Path] = {
    val relSrc = relativize(src).toString
    val relDest = relativize(dest).toString
    val movedPath = Future {
      require(src.toFile.exists(), s"Notebook source at [$src] should exist")
      require(dest.getParent.toFile.exists(), s"Directory at [${dest.getParent()}] should exist")
      require(!dest.toFile.exists(), s"Notebook dest at [$dest] should not exist")
      Files.move(src, dest)
      dest
    }
    for {
      _ <- movedPath
      _ <- gitProvider.add(relDest, commitMsgs.move)
      _ <- gitProvider.remove(relSrc, commitMsgs.move)
    } yield dest
  }
}

// FIXME: move this as a separate class
object ConfigUtils {

  implicit class ConfigOps(val config:Config) extends AnyRef {

    def tryGetString(path: String) : Try[String] = {
      if (config.hasPath(path)) {
        Success(config.getString(path))
      } else {
        Failure(new ConfigurationMissingException(path))
      }
    }

    def tryGetPath(path: String): Try[Config] = {
      if (config.hasPath(path)) {
        Success(config.getConfig(path))
      } else {
        Failure(new ConfigurationMissingException(path))
      }
    }

    def getMandatoryString(path: String) : String = tryGetString(path).get

    def getSafeString(path:String) : Option[String] = tryGetString(path).toOption

    def getSafeList(path:String) : Option[List[String]] = {
      if (config.hasPath(path))
        Some(config.getStringList(path).asScala.toList)
      else
        None
    }
  }

}
