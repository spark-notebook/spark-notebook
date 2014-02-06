/*
 * Copyright (c) 2013  Bridgewater Associates, LP
 *
 * Distributed under the terms of the Modified BSD License.  The full license is in
 * the file COPYING, distributed as part of this software.
 */

package com.bwater.notebook.kernel.pfork

import concurrent.ops
import collection.mutable
import collection.JavaConversions._
import org.apache.commons.exec._
import java.io.File
import java.net.{URLDecoder, URLClassLoader, Socket, ServerSocket}
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import scala.Option

/**
 * I am so sick of this being a thing that gets implemented everywhere. Let's abstract.
 */
class ProcessFork[A: Manifest] {
  import ProcessFork._

  val processClass = manifest[A].erasure
  assert(processClass.getConstructors exists { c =>
    val types = c.getParameterTypes
    singleSeqParameter(types) || allStringParameters(types)
  }, "Class %s can't be used by ProcessFork because it does not have a constructor that can take strings.".format(processClass))

  def workingDirectory = new File(".")
  def heap: Long = defaultHeap
  def stack: Long = -1
  def permGen: Long = -1
  def reservedCodeCache: Long = -1
  def server: Boolean = true
  def debug: Boolean = false // If true, then you will likely get address in use errors spawning multiple processes
  def classPath: IndexedSeq[String] = defaultClassPath
  def classPathString = classPath.mkString(File.pathSeparator)

  def jvmArgs = {
    val builder = IndexedSeq.newBuilder[String]

    def ifNonNeg(value: Long, prefix: String) {
      if (value >= 0) {
        builder += (prefix + value)
      }
    }

    ifNonNeg(heap, "-Xmx")
    ifNonNeg(stack, "-Xss")
    ifNonNeg(permGen, "-XX:MaxPermSize=")
    ifNonNeg(reservedCodeCache, "-XX:ReservedCodeCacheSize=")
    if (server) builder += "-server"
    if (debug) builder ++= IndexedSeq("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005")
    builder.result()
  }

  implicit protected def int2SuffixOps(i: Int) = new SuffixOps(i)

  protected final class SuffixOps(i: Int) {
    def k: Long = i.toLong << 10
    def m: Long = i.toLong << 20
    def g: Long = i.toLong << 30
  }

  def execute(args: String*): ProcessKiller = {
    val cmd = new CommandLine(javaHome + "/bin/java")
      .addArguments(jvmArgs.toArray)
      .addArgument(classOf[ChildProcessMain].getName)
      .addArgument(processClass.getName)
      .addArgument(ProcessFork.serverPort.toString)
      .addArguments(args.toArray)

    log.info("Spawning %s".format(cmd.toString))

    // use environment because classpaths can be longer here than as a command line arg
    val environment = System.getenv + ("CLASSPATH" -> classPathString)

    val exec = new KillableExecutor

    // Change me!
    exec.setWorkingDirectory(workingDirectory)
    exec.execute(cmd, environment, new ExecuteResultHandler {
      def onProcessFailed(e: ExecuteException) {}
      def onProcessComplete(exitValue: Int) {}
    })
    () => exec.kill()
  }
}

object ProcessFork {

  type ProcessKiller = () => Unit

  def defaultClassPath: IndexedSeq[String] = {
    val loader = getClass.getClassLoader.asInstanceOf[URLClassLoader]

    loader.getURLs map {
      u =>
        val f = new File(u.getFile)
        URLDecoder.decode(f.getAbsolutePath, "UTF8")
    }
  }

  def defaultHeap = Runtime.getRuntime.maxMemory

  /* Override to expose ability to forcibly kill the process */
  private class KillableExecutor extends DefaultExecutor {
    val killed = new AtomicBoolean(false)
    setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT) {
      override def start(p: Process) { if (killed.get()) p.destroy() }
    })
    def kill() {
      if (killed.compareAndSet(false, true))
        Option(getExecutorThread()) foreach(_.interrupt())
    }
  }

  /* DK: Bi-directional liveness can be detected via redirected System.in (child), System.out (parent), avoids need for socket... */
  private lazy val serverPort = {
    val ss = new ServerSocket(0)
    ops.spawn {
      try {
        // CY: Not super rigorous, but we think that, if we don't hang onto a reference to the Socket server-side,
        // eventually it'll get GC'ed, which causes the child VM to die.
        val conns = mutable.Buffer[Socket]()

        while (true) {
          val conn = ss.accept()
          conns += conn //TODO: mem/resource leak...
        }
      } catch {
        case e: Throwable => e.printStackTrace() ; throw e
      }
    }
    ss.getLocalPort
  }

  private lazy val javaHome = System.getProperty("java.home")

  private lazy val log = LoggerFactory.getLogger(getClass())

  private[pfork] def main(args: Array[String]) {
    val className = args(0)
    val parentPort = args(1).toInt

    val socket = new Socket("127.0.0.1", parentPort)

    val remainingArgs = args.drop(2).toIndexedSeq

    Class.forName(className).getConstructors collectFirst {
      case c if singleSeqParameter(c.getParameterTypes) => c.newInstance(remainingArgs)
      case c if allStringParameters(c.getParameterTypes) && c.getParameterTypes.length == remainingArgs.length => c.newInstance(remainingArgs: _*)
    } getOrElse log.error("Inconceivable!")

    try {
      // Blocks until parent quits, then throws an exception
      socket.getInputStream.read()
    } finally {
      log.warn("Parent process stopped; exiting.")
      sys.exit(1)
    }
  }

  private def singleSeqParameter(types: Array[Class[_]]) = types.length == 1 && types(0).isAssignableFrom(classOf[IndexedSeq[_]])
  private def allStringParameters(types: Array[Class[_]]) = types.forall(_ == classOf[String])
}
