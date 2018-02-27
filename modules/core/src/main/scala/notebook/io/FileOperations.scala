package notebook.io

import java.nio.file.{Files, Path}

object FileOperations {

  def recursivelyDeletePath(path: Path): Unit = {
    val file = path.toFile
    require(file.exists(), s"Path at [$path] do not exist")

    if (file.isDirectory) {
      val filesInDir = file.listFiles().toSeq
      println(s"All files in directory $path will be deleted:\n $filesInDir")
      filesInDir.map(_.toPath)
        .foreach(recursivelyDeletePath)
    }

    Files.delete(path)
  }

}
