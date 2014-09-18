package org.apache.spark.repl

import scala.reflect._
import scala.reflect.api.{Mirror, Universe, TypeCreator}
import scala.tools.nsc.{io, Properties, Settings, interpreter}
import scala.tools.nsc.interpreter._
import scala.tools.nsc.util.ScalaClassLoader._
import scala.reflect.api.{Mirror, TypeCreator, Universe => ApiUniverse}

import scala.tools.nsc.interpreter._

class HackSparkILoop(out:JPrintWriter) extends SparkILoop(None, out, None) { 
  private def getMaster(): String = {
    val master = this.master match {
      case Some(m) => m
      case None =>
        val envMaster = sys.env.get("MASTER")
        val propMaster = sys.props.get("spark.master")
        propMaster.orElse(envMaster).getOrElse("local[*]")
    }
    master
  }



  private def tagOfStaticClass[T: ClassTag]: u.TypeTag[T] =
    u.TypeTag[T](
      m,
      new TypeCreator {
        def apply[U <: ApiUniverse with Singleton](m: Mirror[U]): U # Type =
          m.staticClass(classTag[T].runtimeClass.getName).toTypeConstructor.asInstanceOf[U # Type]
      })

  override def initializeSpark() {
    /*intp.beQuietDuring {
      command("""
         @transient val sc = org.apache.spark.repl.Main.interp.createSparkContext();
              """)
      command("import org.apache.spark.SparkContext._")
    }
    echo("Spark context available as sc.")*/
  }


  override def process(settings: Settings): Boolean = savingContextLoader {

    if (getMaster() == "yarn-client") System.setProperty("SPARK_YARN_MODE", "true")

    this.settings = settings
    createInterpreter()

    // sets in to some kind of reader depending on environmental cues
    in ={
        // some post-initialization
        chooseReader(settings) match {
          case x: SparkJLineReader => addThunk(x.consoleReader.postInit) ; x
          case x                   => x
        }
    }
    lazy val tagOfSparkIMain = tagOfStaticClass[org.apache.spark.repl.SparkIMain]
    // Bind intp somewhere out of the regular namespace where
    // we can get at it in generated code.
    addThunk(intp.quietBind(NamedParam[SparkIMain]("$intp", intp)(tagOfSparkIMain, classTag[SparkIMain])))
    addThunk({
      import scala.tools.nsc.io._
      import Properties.userHome
      import scala.compat.Platform.EOL
      val autorun = replProps.replAutorunCode.option flatMap (f => io.File(f).safeSlurp())
      if (autorun.isDefined) intp.quietRun(autorun.get)
    })

    addThunk(printWelcome())
    addThunk(initializeSpark())

    // it is broken on startup; go ahead and exit
    if (intp.reporter.hasErrors)
      return false

    // This is about the illusion of snappiness.  We call initialize()
    // which spins off a separate thread, then print the prompt and try
    // our best to look ready.  The interlocking lazy vals tend to
    // inter-deadlock, so we break the cycle with a single asynchronous
    // message to an actor.
    if (isAsync) {
      intp initialize initializedCallback()
      createAsyncListener() // listens for signal to run postInitialization
    }
    else {
      intp.initializeSynchronous()
      postInitialization()
    }
    // printWelcome()

    loadFiles(settings)

    //try loop()
    //catch AbstractOrMissingHandler()
    //finally closeInterpreter()

    true
  }



}
