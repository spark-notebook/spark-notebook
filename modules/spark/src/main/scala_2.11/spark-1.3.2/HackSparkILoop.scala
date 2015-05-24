package scala
package tools.nsc
package interpreter

import scala.language.{ implicitConversions, existentials }
import scala.annotation.tailrec
import Predef.{ println => _, _ }
import interpreter.session._
import StdReplTags._
import scala.reflect.api.{Mirror, Universe, TypeCreator}
import scala.util.Properties.{ jdkHome, javaVersion, versionString, javaVmName }
import scala.tools.nsc.util.{ ClassPath, Exceptional, stringFromWriter, stringFromStream }
import scala.reflect.{ClassTag, classTag}
import scala.reflect.internal.util.{ BatchSourceFile, ScalaClassLoader }
import ScalaClassLoader._
import scala.reflect.io.{ File, Directory }
import scala.tools.util._
import scala.collection.generic.Clearable
import scala.concurrent.{ ExecutionContext, Await, Future, future }
import ExecutionContext.Implicits._
import java.io.{ BufferedReader, FileReader }

import scala.tools.nsc.interpreter._

class HackSparkILoop(out:JPrintWriter) extends SparkILoop(None, out) {
  override def initializeSpark() {
    /*intp.beQuietDuring {
      command("""
         @transient val sc = org.apache.spark.repl.Main.interp.createSparkContext();
              """)
      command("import org.apache.spark.SparkContext._")
    }
    echo("Spark context available as sc.")*/
  }

  val classServer = {
    val s = org.apache.spark.Boot.classServer
    s.start
    s
  }

  private def loopPostInit() {
    // Bind intp somewhere out of the regular namespace where
    // we can get at it in generated code.
    intp.quietBind(NamedParam[SparkIMain]("$intp", intp)(tagOfStaticClass[SparkIMain], classTag[SparkIMain]))
    // Auto-run code via some setting.
    ( replProps.replAutorunCode.option
      flatMap (f => io.File(f).safeSlurp())
      foreach (intp quietRun _)
      )
    // classloader and power mode setup
    intp.setContextClassLoader()
    if (isReplPower) {
     // replProps.power setValue true
     // unleashAndSetPhase()
     // asyncMessage(power.banner)
    }
    // SI-7418 Now, and only now, can we enable TAB completion.
    in match {
      case x: JLineReader => x.consoleReader.postInit
      case _              =>
    }
  }

  override def process(settings: Settings): Boolean = savingContextLoader {
    this.settings = settings
    createInterpreter()

    // sets in to some kind of reader depending on environmental cues
    in = chooseReader(settings)// in0.fold(chooseReader(settings))(r => SimpleReader(r, out, interactive = true))
    globalFuture = Future {
      intp.initializeSynchronous()
      loopPostInit()
      !intp.reporter.hasErrors
    }
    import scala.concurrent.duration._
    Await.ready(globalFuture, 10 seconds)
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