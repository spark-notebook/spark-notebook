package controllers

import java.util.UUID

import scala.util.Try
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import com.typesafe.config._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import notebook._
import notebook.server._
import notebook.kernel.remote._

object AppUtils {
  import play.api.Play.current

  lazy val config = NotebookConfig(current.configuration.getConfig("manager").get)
  lazy val nbm = new NotebookManager(config.projectName, config.notebooksDir)
  lazy val notebookServerConfig = current.configuration.getConfig("notebook-server").get.underlying

  lazy val kernelSystem =  ActorSystem( "NotebookServer",
                                        notebookServerConfig,
                                        play.api.Play.classloader // this resolves the Play classloader problems w/ remoting
                                      )
}

case class Crumb(url:String="", name:String="")
case class Breadcrumbs(home:String="/", crumbs:List[Crumb] = Nil)


object Application extends Controller {

  lazy val config = AppUtils.config
  lazy val nbm = AppUtils.nbm
  lazy val notebookServerConfig = AppUtils.notebookServerConfig

  implicit def kernelSystem =  AppUtils.kernelSystem

  val kernelIdToCalcService = collection.mutable.Map[String, CalcWebSocketService]()

  val project = "Spark Notebook" //TODO from application.conf
  val base_project_url = "/"
  val base_kernel_url = "/"
  val base_observable_url = "observable" // TODO: Ugh...
  val read_only = false.toString
  val terminals_available = false.toString //TODO

  def configTree() = Action {
    Ok(Json.obj())
  }

  def configCommon() = Action {
    Ok(Json.obj())
  }

  def configNotebook() = Action {
    Ok(Json.obj())
  }

  def kernelSpecs() = Action {
    Ok(Json.parse(
        """
        |{
        |  "kernelspecs": {
        |    "spark": {
        |      "name": "spark",
        |      "resources": {},
        |      "spec" : {
        |        "language": "scala",
        |        "display_name": "Apache Spark",
        |
        |        "language_info": {
        |          "name" : "scala",
        |          "file_extension" : "scala",
        |          "codemirror_mode" : "text/x-scala"
        |        }
        |      }
        |    }
        |  }
        |}
        |""".stripMargin.trim
      )
    )
  }

  private [this] def newSession(kernelId:Option[String]=None, notebookPath:Option[String]=None) = {
    val kId = kernelId.getOrElse(UUID.randomUUID.toString)
    val compilerArgs = config.kernel.compilerArgs.toList
    val initScripts = config.kernel.initScripts.toList
    val kernel = new Kernel(config.kernel.config.underlying, kernelSystem)
    KernelManager.add(kId, kernel)

    val r = Reads.map[String]

    // Load the notebook → get the metadata → get custom → git it to CalcWebSocketService
    val custom:Option[Map[String, String]] = for {
      p <- notebookPath
      n <- nbm.load(p.dropRight(".snb".size))
      m <- n.metadata
      c <- m.custom
      _ = Logger.info("CUSTOM::: " + c)
      map <- r.reads(c).asOpt
    } yield map
    val service = new CalcWebSocketService(kernelSystem, custom, initScripts, compilerArgs, kernel.remoteDeployFuture)
    kernelIdToCalcService += kId -> service

    // todo add MD?
    Json.parse(
      s"""
      |{
      |  "id": "$kId",
      |  "name": "spark",
      |  "language_info": {
      |    "name" : "Scala",
      |    "file_extension" : "scala",
      |    "codemirror_mode" : "text/x-scala"
      |  }
      |}
      |""".stripMargin.trim
    )
  }

  def createSession() = Action(parse.tolerantJson)/* → posted as urlencoded form oO */ { request =>
    val json:JsValue = request.body
    val kernelId = Try((json \ "kernel" \ "id").as[String]).toOption
    val notebookPath = Try((json \ "notebook" \ "path").as[String]).toOption
    val k = newSession(kernelId, notebookPath)
    Ok(Json.obj("kernel" → k))
  }

  def sessions() = Action {
    Ok(Json.obj()) // TODO using kernelIdToCalcService
  }


  val clustersActor = kernelSystem.actorOf(Props( new NotebookClusters ))
  implicit val GetClustersTimeout = Timeout(60 seconds)

  def clusters() = Action.async {
    implicit val ec = kernelSystem.dispatcher
    (clustersActor ? NotebookClusters.All).map { case all:List[JsObject] =>
      Ok(JsArray(all))
    }
  }

  def addCluster() = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    implicit val ec = kernelSystem.dispatcher
    json match {
      case o:JsObject =>
        (clustersActor ? NotebookClusters.Add((json \ "name").as[String], o)).map { case cluster:JsObject =>
          Ok(cluster)
        }
      case _ => Future {
        BadRequest("Add cluster needs an object, got: " + json)
      }
    }
  }

  def contents(`type`:String) = Action {
    //todo → dirs
    val a = Option(config.notebooksDir.listFiles.toList)
      .getOrElse(Nil)
      .filter(_.isFile)
      .map { f =>
        f.getName
      }.map { n =>
        Json.obj(
          "type" → "file",
          "name" → n.dropRight(".snb".size),
          "path" → n //todo → build relative path
        )
      }

    Ok(Json.obj(
      "content" → a
    ))
  }

  def content(`type`:String, snb:String) = Action {
    require(`type` == "notebook") // ?
    Logger.info("content: " + snb)
    val name = if (snb.endsWith(".snb")) {snb.dropRight(".snb".size)} else {snb}
    getNotebook(name, name+".snb", "json")
  }

  def newNotebook() = Action(parse.tolerantText) { request =>
    val text = request.body
    val customMetadata = Try(text).flatMap(j => Try(Json.parse(j) \ "custom")).toOption flatMap {
      case x:JsObject  => Some(x)
      case `JsNull`    => None
      case y           => throw new RuntimeException("Cannot created notebook with " + y)
    }

    val name = nbm.newNotebook(customMetadata)
    Logger.info("new: " + name)
    Redirect(routes.Application.content("notebook", name))
  }

  def openNotebook(name:String) = Action { request =>
    val path = name
    Logger.info(s"View notebook. Name is '$name' at '$path'")
    val ws_url = s"ws:/${request.host}/ws"

    Ok(views.html.notebook(
      nbm.name /*project?*/,
      Map(
        "base-url" -> base_project_url,
        "ws-url" -> ws_url,

        "base-project-url" -> base_project_url,
        "base-kernel-url" -> base_kernel_url,
        "base-observable-url" -> s"$ws_url/$base_observable_url",
        "read-only" -> read_only,
        //"notebook-id" -> id /*getOrElse nbm.notebookId(name))*/,
        "notebook-name" -> name,
        "notebook-path" -> path,
        "notebook-writable" -> "true"
      )
    ))
  }

  private [this] def closeKernel(kernelId:String) = {
    KernelManager.get(kernelId).foreach{ k =>
      Logger.info(s"Closing kernel $kernelId")
      k.shutdown()
      KernelManager.remove(kernelId)
    }
  }

  def openKernel(kernelId:String) =  ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketKernelActor.props(channel, kernelIdToCalcService(kernelId)),
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      Logger.info(s"Closing websockets for kernel $kernelId")
      closeKernel(kernelId)
      ref ! akka.actor.PoisonPill
    }
  )

  def restartKernel(kernelId:String) = Action { request =>
    //shouldn't do anything since onClose should be called in openKernel (stopChannels is call in the front)
    // /!\ this won't kill the underneath actor!!!
    closeKernel(kernelId)

    // TODO → the notebookPath here!!!
    Ok(newSession(notebookPath=None))
  }

  def listCheckpoints(snb:String) = Action { request =>
    Ok(Json.parse(
      """
      |[
      | { "id": "TODO", "last_modified": "2015-01-02T13:22:01.751Z" }
      |]
      |""".stripMargin.trim
    ))
  }

  def saveCheckpoint(snb:String) = Action { request =>
    //TODO
    Ok(Json.parse(
      """
      |[
      | { "id": "TODO", "last_modified": "2015-01-02T13:22:01.751Z" }
      |]
      |""".stripMargin.trim
    ))
  }

  def renameNotebook(snb:String) = Action(parse.tolerantJson) { request =>
    val notebook = (request.body \ "path").as[String]
    try {
      nbm.rename(snb, notebook)

      Ok(Json.obj(
        "type" → "file",
        "name" → notebook,
        "path" → notebook //todo → rebuild relative path
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def saveNotebook(snb:String) = Action(parse.tolerantJson(maxLength = 1024 * 1024 /*1Mb*/)) { request =>
    val notebook = NBSerializer.fromJson(request.body \ "content")
    try {
      nbm.save(snb.dropRight(".snb".size), snb, notebook, true)

      Ok(Json.obj(
        "type" → "file",
        "name" → snb,
        "path" → snb //todo → rebuild relative path
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def dlNotebookAs(path:String, format:String) = Action {
    getNotebook(path.dropRight(".snb".size), path, format)
  }

  def dash(title:String) = Action {
    Ok(views.html.projectdashboard(
      title,
      Map(
        "project" → project,
        "base-project-url" → base_project_url,
        "base-kernel-url" → base_kernel_url,
        "read-only" → read_only,
        "base-url" → base_project_url,
        "notebook-path" → base_kernel_url,
        "terminals-available" → terminals_available
      ),
      Breadcrumbs()
    ))
  }

  def openObservable(contextId:String) = ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketObservableActor.props(channel, contextId),
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      Logger.info(s"Closing observable $contextId")
      ref ! akka.actor.PoisonPill
    }
  )

  def getNotebook(name: String, path: String, format: String) = {
    try {
      Logger.info(s"getNotebook: name is '$name', path is '$path' and format is '$format'")
      val response = nbm.getNotebook(name, path).map { case (lastMod, name, data) =>
        format match {
          case "json" =>
            val j = Json.parse(data)
            val json = Json.obj(
              "content" → j,
              "name" → name,
              "path" → path, //FIXME
              "writable" -> true //TODO
            )
            Ok(json).withHeaders(
              "Content-Disposition" → s"""attachment; filename="$path" """,
              "Last-Modified" → lastMod
            )
          case "scala" =>
            val nb = NBSerializer.fromJson(Json.parse(data))
            val code = nb.cells.map { cells =>
              val cs = cells.collect {
                case NBSerializer.CodeCell(md, "code", i, Some("scala"), _, _) => i
                case NBSerializer.CodeCell(md, "code", i, None, _, _) => i
              }
              val fc = cs.map(_.split("\n").map { s => s"  $s" }.mkString("\n")).mkString("\n\n  /* ... new cell ... */\n\n").trim
              val code = s"""
              |object Cells {
              |  $fc
              |}
              """.stripMargin
              code
            }.getOrElse(""" //NO CELLS! """)

            Ok(code).withHeaders(
              "Content-Disposition" → s"""attachment; filename="$name.scala" """,
              "Last-Modified" → lastMod
            )
          case _ => InternalServerError(s"Unsupported format $format")
        }
      }

      response getOrElse NotFound(s"Notebook '$name' not found at $path.")
    } catch {
      case e: Exception =>
        Logger.error("Error accessing notebook %s".format(name), e)
        InternalServerError
    }
  }

  // util
  object ImperativeWebsocket {

    def using[E: WebSocket.FrameFormatter](
            onOpen: Channel[E] => ActorRef,
            onMessage: (E, ActorRef) => Unit,
            onClose: ActorRef => Unit,
            onError: (String, Input[E]) => Unit = (e: String, _: Input[E]) => (Logger.error(e))
    ): WebSocket[E, E] = {
      implicit val sys = kernelSystem.dispatcher

      val promiseIn = Promise[Iteratee[E, Unit]]

      val out = Concurrent.unicast[E](
        onStart = channel => {
          val ref = onOpen(channel)
          val in = Iteratee.foreach[E] { message =>
            onMessage(message, ref)
          } map (_ => onClose(ref))
          promiseIn.success(in)
        },
        onError = onError
      )

      WebSocket.using[E](_ => (Iteratee.flatten(promiseIn.future), out))
    }

  }

}
