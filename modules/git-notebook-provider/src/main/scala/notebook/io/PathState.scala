package notebook.io

import java.io.{File, IOException}
import java.nio.file.{Path, Paths}
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}


sealed trait PathState {
  def path:Path
  def toFile:File = path.toFile
}

case class EmptyPath(val path:Path) extends PathState
case class NonEmptyNonGitPath(val path:Path) extends PathState
case class NonEmptyGitPath(val path:Path) extends PathState

object PathState {
  def apply(path : String): Try[PathState] = {
    val targetPath = Paths.get(path)
    val pathAsFile = targetPath.toFile
    if (!pathAsFile.exists()) {
      create(targetPath).map(_ => EmptyPath(targetPath))
    } else {
      val content = pathAsFile.list()
      if (content.isEmpty) {
        Success(EmptyPath(targetPath))
      } else {
        if (content.contains(".git")) {
          Success(NonEmptyGitPath(targetPath))
        } else {
          Success(NonEmptyNonGitPath(targetPath))
        }
      }
    }
  }

  def create(path:Path) : Try[Unit] = {
    try {
      FileUtils.forceMkdir(path.toFile)
      Success(())
    } catch {
      case ex:IOException => new Failure (new ConfigurationCorruptException(s"Failed to create a directory under [${path}]"))
    }
  }
}

