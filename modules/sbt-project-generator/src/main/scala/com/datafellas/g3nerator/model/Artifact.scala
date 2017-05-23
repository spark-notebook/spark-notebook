package com.datafellas.g3nerator.model

import java.io.{File, FileWriter, PrintWriter}

import com.typesafe.config.Config

import scala.util.{Failure, Try}

trait Materializer {
  def materialize(file: File, content:String) : Try[Unit]
}

class FileSystemMaterializer() extends Materializer {
  override def materialize(file: File, content:String) = {
    if (file.isDirectory) {
      Failure(new RuntimeException("Artifact target cannot be a directory: " + file.getAbsolutePath))
    } else {
      Try {
        val writer = new PrintWriter(new FileWriter(file))
        writer.print(content)
        writer.close
      }
    }
  }
}

class Artifact[S <: Artifact.State] (val fd: File, val content: String) {

  import Artifact.State._

  def materialize(m: Materializer)(implicit ev: S =:= Unmaterialized): Try[Artifact[Materialized]] = {
    m.materialize(fd, content).map(_ => new Artifact[Materialized](fd, content))
  }

  override def equals(other: Any): Boolean = {
    other match {
      case o:Artifact[_] => (this.fd == o.fd) && (this.content == o.content)
      case _ => false
    }
  }

  override def hashCode: Int = {
    17 * fd.hashCode + 31 * content.hashCode
  }
}

object Artifact {
  sealed trait State
  object State {
    sealed trait Unmaterialized extends State
    sealed trait Materialized extends State
  }
  def apply(fd:File, content:String) : Artifact[State.Unmaterialized] = new Artifact[State.Unmaterialized](fd, content)
}
