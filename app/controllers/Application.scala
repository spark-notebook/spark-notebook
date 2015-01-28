package controllers

import java.util.UUID

import scala.util.Try
import scala.concurrent.Promise

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel

import com.typesafe.config._

import akka.actor._

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
        |        "display_name": "Apache Spark"
        |      }
        |    }
        |  }
        |}
        |""".stripMargin.trim
      )
    )
  }


  def createSession() = Action(parse.tolerantJson)/* → posted as urlencoded form oO */ { request =>
    //{"notebook":{"path":"ADAM"},"kernel":{"id":null}}:
    val json:JsValue = request.body
    val kernelId = Try((json \ "kernel" \ "id").as[String]).toOption.getOrElse(UUID.randomUUID.toString)

    val compilerArgs = config.kernel.compilerArgs.toList
    val initScripts = config.kernel.initScripts.toList
    val kernel = new Kernel(config.kernel.config.underlying, kernelSystem)
    KernelManager.add(kernelId, kernel)

    val service = new CalcWebSocketService(kernelSystem, initScripts, compilerArgs, kernel.remoteDeployFuture)
    kernelIdToCalcService += kernelId -> service

    Ok(Json.parse(
        s"""
        |{
        |  "kernel": {
        |    "id": "$kernelId",
        |    "name": "spark"
        |  }
        |}
        |""".stripMargin.trim
      )
    )
  }

  def sessions() = Action {
    Ok(Json.obj())
  }

  def clusters() = Action {
    Ok(Json.obj())
  }

  def contents(`type`:String) = Action {
    //todo → dirs
    val a = Option(config.notebooksDir.listFiles.toList)
      .getOrElse(Nil)
      .filter(_.isFile)
      .map { f =>
        f.getName.dropRight(".snb".size)
      }.map { n =>
        Json.obj(
          "type" → "file",
          "name" → n,
          "path" → n //todo → build relative path
        )
      }

    Ok(Json.obj(
      "content" → a
    ))
  }

  def content(`type`:String, snb:String) = Action {
    require(`type` == "notebook") // ?

    getNotebook(snb, snb, "json")
  }

  def newNotebook() = Action {
    val id = nbm.newNotebook()
    val name = nbm.idToName(id)
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

  def openKernel(kernelId:String, sessionId:String, pchannel:String="fixme") =  ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketKernelActor.props(channel, pchannel, kernelIdToCalcService(kernelId)),
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      Logger.info(s"Closing websockets for kernel $kernelId, $pchannel")
      KernelManager.get(kernelId).foreach{ k =>
        Logger.info(s"Closing kernel $kernelId, $pchannel")
        k.shutdown()
      }
      ref ! akka.actor.PoisonPill
    }
  )

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

  def saveNotebook(snb:String) = Action(parse.tolerantJson) { request =>
    val notebook = NBSerializer.fromJson(request.body \ "content")
    try {
      nbm.save(Some(snb), snb, notebook, true)

      Ok(Json.obj(
        "type" → "file",
        "name" → snb,
        "path" → snb //todo → rebuild relative path
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
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










  def listNotebooks = Action {
    Ok(nbm.listNotebooks)
  }

  def newNotebookOld = Action {
    val id = nbm.newNotebook()
    val name = nbm.idToName(id)
    Redirect(routes.Application.viewNotebook(name, id))
  }

  def viewNotebook(name:String, id:String) = Action { request =>
    Logger.info(s"View notebook. Name is '$name', id id '$id'")
    val ws_url = s"ws:/${request.host}"

    Ok(views.html.notebook(
      nbm.name /*project?*/,
      Map(
        "base-project-url" -> base_project_url,
        "base-kernel-url" -> base_kernel_url,
        "base-observable-url" -> s"$ws_url/$base_observable_url",
        "read-only" -> read_only,
        "notebook-id" -> id /*getOrElse nbm.notebookId(name))*/,
        "notebook-name" -> name
      )
    ))
  }

  def saveNotebookOld(name:String, id:String, force:Boolean) = Action(parse.json) { request =>
    val notebook = NBSerializer.fromJson(request.body)
    try {
      nbm.save(Some(id), name, notebook, force)
      Ok("saved " + id)
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def dlNotebook(name:String, id:String) = Action {
    getNotebook(id, name, "json")
  }

  def dlNotebookAs(name:String, id:String, format:String) = Action {
    getNotebook(id, name, format)
  }

  def createKernel = Action { implicit request:RequestHeader =>
    startKernel(UUID.randomUUID.toString)
  }

  def openObservable(contextId:String) = ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketObservableActor.props(channel, contextId),
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      Logger.info(s"Closing observable $contextId")
      ref ! akka.actor.PoisonPill
    }
  )

  def openKernelOld(kernelId:String, pchannel:String) =  ImperativeWebsocket.using[JsValue](
    onOpen = channel => {
      WebSocketKernelActor.props(channel, pchannel, kernelIdToCalcService(kernelId))
    },
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      Logger.info(s"Closing websockets for kernel $kernelId, $pchannel")
      KernelManager.get(kernelId).foreach{ k =>
        Logger.info(s"Closing kernel $kernelId, $pchannel")
        k.shutdown()
      }
      ref ! akka.actor.PoisonPill
    }
  )

  def getNotebook(id: String, name: String, format: String) = {
    try {
      Logger.info(s"getNotebook: id is '$id', name is '$name' and format is '$format'")
      val response = nbm.getNotebook(Some(id), name).map { case (lastMod, name, data) =>
        format match {
          case "json" =>
            val j = Json.parse(data)
            val json = Json.obj(
              "content" → j,
              "name" → name,
              "path" → name, //FIXME
              "writable" -> true //TODO
            )
            Ok(json).withHeaders(
              "Content-Disposition" → s"""attachment; filename="$name.snb" """,
              "Last-Modified" → lastMod
            )
          case "scala" =>
            val nb = NBSerializer.fromJson(Json.parse(data))
            val code = nb.cells.map { cells =>
              val cs = cells.collect { case NBSerializer.CodeCell(md, "code", i, Some("scala"), _, _) => i }
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

      response getOrElse NotFound("Notebook not found.")
    } catch {
      case e: Exception =>
        Logger.error("Error accessing notebook %s".format(name), e)
        InternalServerError
    }
  }

  def startKernel(kernelId: String)(implicit request:RequestHeader) = {
    val compilerArgs = config.kernel.compilerArgs.toList
    val initScripts = config.kernel.initScripts.toList
    val kernel = new Kernel(config.kernel.config.underlying, kernelSystem)
    KernelManager.add(kernelId, kernel)

    val service = new CalcWebSocketService(kernelSystem, initScripts, compilerArgs, kernel.remoteDeployFuture)
    kernelIdToCalcService += kernelId -> service

    val json = Json.obj(
      "kernel_id" -> kernelId,
      "ws_url" -> s"ws://${request.host}"
    )
    Ok(json)
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