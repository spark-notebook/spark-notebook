package org.apache.spark.repl

import scala.reflect._
import scala.reflect.api.{Mirror, Universe, TypeCreator}
import scala.tools.nsc.{io, Properties, Settings, interpreter}
import scala.tools.nsc.interpreter._
import scala.tools.nsc.util.ScalaClassLoader._
import scala.reflect.api.{Mirror, TypeCreator, Universe => ApiUniverse}
import scala.concurrent.{ ExecutionContext, Await, Future, future }
import ExecutionContext.Implicits._


import scala.tools.nsc.interpreter._

class HackSparkILoop(out:JPrintWriter) extends org.apache.spark.repl.SparkILoop(None, out) {

  val classServer = {
    val s = org.apache.spark.Boot.classServer
    s.start
    s
  }

  override def initializeSpark() {
    // done using the metadata and init.sc
  }

  override def printWelcome() {
    //
  }

  override def process(settings: Settings): Boolean = savingContextLoader {
    this.settings = settings
    createInterpreter()

    // sets in to some kind of reader depending on environmental cues
    in = chooseReader(settings)// in0.fold(chooseReader(settings))(r => SimpleReader(r, out, interactive = true))
    val globalFuture = Future {
      intp.initializeSynchronous()
      //loopPostInit()
      !intp.reporter.hasErrors
    }
    import scala.concurrent.duration._
    Await.ready(globalFuture, 1 minute)
    //printWelcome()
    //initializeSpark()
    loadFiles(settings)

    /**
    try loop()
    catch AbstractOrMissingHandler()
    finally closeInterpreter()
    */

    true
  }
}