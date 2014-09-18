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
import java.io.{EOFException, ObjectInputStream, ObjectOutputStream, File}
import java.net._
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import scala.Option
import com.sun.org.apache.xpath.internal.functions.FuncTrue
import scala.concurrent._
import duration.Duration
import collection.mutable.ListBuffer
import java.util.concurrent.Executors
import org.apache.log4j.PropertyConfigurator

trait ForkableProcess {
  /**
   * Called in the remote VM. Can return any useful information to the server through the return
   * @param args
   * @return
   */
  def init(args: Seq[String]):String
  def waitForExit()
}


/**
 * I am so sick of this being a thing that gets implemented everywhere. Let's abstract.
 */
class BetterFork[A <: ForkableProcess : Manifest](executionContext: ExecutionContext ) {
  private implicit val ec = executionContext

  import BetterFork._

  val processClass = manifest[A].erasure

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

  def execute(args: String*): Future[ProcessInfo] = {
    /* DK: Bi-directional liveness can be detected via redirected System.in (child), System.out (parent), avoids need for socket... */
    //val port = 3344+serverSockets.size
    //println(port)
    val ss = new ServerSocket(0)
    //println(ss.getLocalPort.toString)
    val cmd = new CommandLine(javaHome + "/bin/java")
      .addArguments(jvmArgs.toArray)
      .addArgument(classOf[ChildProcessMain].getName)
      .addArgument(processClass.getName)
      .addArgument(ss.getLocalPort.toString)
      .addArguments(args.toArray)

    Future {
      log.info("Spawning %s".format(cmd.toString))

      // use environment because classpaths can be longer here than as a command line arg
      val environment = System.getenv + ("CLASSPATH" -> classPathString)
      val exec = new KillableExecutor

      val completion = Promise[Int]
      exec.setWorkingDirectory(workingDirectory)
      exec.execute(cmd, environment, new ExecuteResultHandler {
        def onProcessFailed(e: ExecuteException) {}
        def onProcessComplete(exitValue: Int) { completion.success(exitValue)}
      })
      val socket = ss.accept()
      serverSockets += socket
      try {
        val ois = new ObjectInputStream(socket.getInputStream)
        val resp = ois.readObject().asInstanceOf[String]
        new ProcessInfo(() => exec.kill(), resp, completion.future)
      } catch {
        case ex:SocketException => throw new ExecuteException("Failed to start process %s".format(cmd), 1, ex)
        case ex:EOFException =>    throw new ExecuteException("Failed to start process %s".format(cmd), 1, ex)
      }
    }
  }
}

class ProcessInfo(killer: () => Unit, val initReturn: String, val completion: Future[Int]) {
  def kill() { killer() }
}

object BetterFork {

  // Keeps server sockets around so they are not GC'd
  private val serverSockets = new ListBuffer[Socket]()

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

  private lazy val javaHome = System.getProperty("java.home")

  private lazy val log = LoggerFactory.getLogger(getClass())

  private[pfork] def main(args: Array[String]) {
    val className = args(0)
    val parentPort = args(1).toInt

    PropertyConfigurator.configure(getClass().getResource("/log4j.subprocess.properties"))

    log.info("Remote process starting")
    val socket = new Socket("127.0.0.1", parentPort)

    val remainingArgs = args.drop(2).toIndexedSeq

    val hostedClass = Class.forName(className).newInstance().asInstanceOf[ForkableProcess]

    val result = hostedClass.init(remainingArgs)

    val oos = new ObjectOutputStream(socket.getOutputStream)
    oos.writeObject(result)
    oos.flush()

    val executorService = Executors.newFixedThreadPool(10)
    implicit val ec = ExecutionContext.fromExecutorService(executorService)

    val parentDone = Future { socket.getInputStream.read() }
    val localDone = Future{ hostedClass.waitForExit() }

    val done = Future.firstCompletedOf(Seq(parentDone, localDone))

    try {
      Await.result(done, Duration.Inf)
    } finally {
      log.warn("Parent process stopped; exiting.")
      sys.exit(0)
    }
  }
}
