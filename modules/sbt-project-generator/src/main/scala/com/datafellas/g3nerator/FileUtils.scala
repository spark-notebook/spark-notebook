package com.datafellas.g3nerator

import java.io.{File, FileWriter, PrintWriter}

object FileUtils {
  def atS(f:String, p:String):File = at(new File(f), p)

  implicit class DirOps(val dir:File) extends AnyVal {
    def /(path : String) : File = at(dir, path)
    def apply(filename: String) : File = new File(dir, filename)
  }

  def at(f:File, p:String):File = {
    p.split("/").toList match {
      case Nil | List("") =>
        f.mkdirs
        f
      case x::rest =>
        at(new File(f, x), rest.mkString("/"))
    }
  }

  sealed trait AppendMode {
    def legacyValue: Boolean
  }
  case class AppendValue(legacyValue:Boolean) extends AppendMode
  object Append extends AppendValue (true)
  object NoAppend extends AppendValue (false)

  def writeTo(t:File, appendMode: AppendMode = NoAppend)(s:String) = {
    val w = new PrintWriter(new FileWriter(t, appendMode.legacyValue))
    w.println(s)
    w.flush()
    w.close()
  }

}