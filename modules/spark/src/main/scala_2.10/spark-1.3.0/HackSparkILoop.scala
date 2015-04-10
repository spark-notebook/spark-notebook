package org.apache.spark.repl

import scala.reflect._
import scala.reflect.api.{Mirror, Universe, TypeCreator}
import scala.tools.nsc.{io, Properties, Settings, interpreter}
import scala.tools.nsc.interpreter._
import scala.tools.nsc.util.ScalaClassLoader._
import scala.reflect.api.{Mirror, TypeCreator, Universe => ApiUniverse}

import scala.tools.nsc.interpreter._

class HackSparkILoop(out:JPrintWriter) extends SparkILoop(None, out, None) { loop:SparkILoop =>
  def getMaster(): String = {
    val master = this.master match {
      case Some(m) => m
      case None =>
        val envMaster = sys.env.get("MASTER")
        val propMaster = sys.props.get("spark.master")
        propMaster.orElse(envMaster).getOrElse("local[*]")
    }
    master
  }
  //override var intp: SparkIMain = _

  // classpath entries added via :cp
  // CP DOESN'T WORK WITH THIS → var addedClasspath: String = ""
  //var addedClasspath: String = ""

  val addedClasspathGS:(() => String, String=>Unit) = {
    val getter = classOf[SparkILoop].getDeclaredMethods.find(_.getName == "org$apache$spark$repl$SparkILoop$$addedClasspath").get
    val get = () => getter.invoke(loop).asInstanceOf[String]

    val setter = classOf[SparkILoop].getDeclaredMethods.find(_.getName == "org$apache$spark$repl$SparkILoop$$addedClasspath_$eq").get
    val set = (s:String) => { setter.invoke(loop, s); ()}

    (get, set)
  }

  def addCps(jars:List[String]) = {
    import scala.tools.nsc.util.ClassPath
    var s:String = addedClasspathGS._1()
    jars foreach { jar =>
      val f = scala.tools.nsc.io.File(jar).normalize
      s = ClassPath.join(s, f.path)
    }
    addedClasspathGS._2(s)
  }


  /** A reverse list of commands to replay if the user requests a :replay */
  var replayCommandStack: List[String] = Nil

  /** A list of commands to replay if the user requests a :replay */
  def replayCommands = replayCommandStack.reverse

  /** Record a command for replay should the user request a :replay */
  def addReplay(cmd: String) = replayCommandStack ::= cmd

  val u: scala.reflect.runtime.universe.type = scala.reflect.runtime.universe
  val m = u.runtimeMirror(getClass.getClassLoader)
  def tagOfStaticClass[T: ClassTag]: u.TypeTag[T] =
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

  var in: InteractiveReader = _   // the input stream from which commands come

  /** Tries to create a JLineReader, falling back to SimpleReader:
   *  unless settings or properties are such that it should start
   *  with SimpleReader.
   */
  def chooseReader(settings: Settings): InteractiveReader = {
    if (settings.Xnojline.value || Properties.isEmacsShell)
      SimpleReader()
    else try new SparkJLineReader(
      if (settings.noCompletion.value) NoCompletion
      else new SparkJLineCompletion(intp)
    )
    catch {
      case ex @ (_: Exception | _: NoClassDefFoundError) =>
        echo("Failed to created SparkJLineReader: " + ex + "\nFalling back to SimpleReader.")
        SimpleReader()
    }
  }

  // runs :load `file` on any files passed via -i
  def loadFiles(settings: Settings) = settings match {
    case settings: SparkRunnerSettings =>
      for (filename <- settings.loadfiles.value) {
        val cmd = ":load " + filename
        command(cmd)
        addReplay(cmd)
        echo("")
      }
    case _ =>
  }

  def reset() {
    intp.reset()
    // unleashAndSetPhase()
  }
  def replay() {
    reset()
    if (replayCommandStack.isEmpty)
      echo("Nothing to replay.")
    else for (cmd <- replayCommands) {
      echo("Replaying: " + cmd)  // flush because maybe cmd will have its own output
      command(cmd)
      echo("")
    }
  }

  def process(settings: Settings): Boolean = savingContextLoader {

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
      // ??? intp.getInterpreterClassLoader
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
