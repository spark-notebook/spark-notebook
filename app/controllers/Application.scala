package controllers

import java.util.UUID

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.json._
import play.api.libs.iteratee._

import com.typesafe.config._

import akka.actor._

import notebook._
import notebook.server._
import notebook.kernel.remote._

object Application extends Controller {
  lazy val config = NotebookConfig(Play.current.configuration.getConfig("manager").get)
  lazy val nbm = new NotebookManager(config.projectName, config.notebooksDir)

  implicit val kernelSystem =  ActorSystem( "NotebookServer",
                                            ConfigFactory.load("notebook-server")
                        /*AkkaConfigUtils.optSecureCookie(
                          ConfigFactory.load("notebook-server"),
                          akka.util.Crypt.generateSecureCookie
                        )*/
                      )

  val kernelIdToCalcService = collection.mutable.Map[String, CalcWebSocketService]()

  val project = "Spark Notebook" //TODO from application.conf
  val base_project_url = "/"
  val base_kernel_url = "/"
  val base_observable_url = "observable" // TODO: Ugh...
  val read_only = false.toString

  def dash(title:String) = Action {
    Ok(views.html.projectdashboard(title, Map(
      "project" → project,
      "base-project-url" → base_project_url,
      "base-kernel-url" → base_kernel_url,
      "read-only" → read_only
    )))
  }


  def listNotebooks = Action {
    Ok(nbm.listNotebooks)
  }

  def newNotebook = Action {
    val id = nbm.newNotebook()
    val name = nbm.idToName(id)
    Redirect(routes.Application.viewNotebook(name, id))
  }

  def viewNotebook(name:String, id:String) = Action { request =>
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

  def saveNotebook(name:String, id:String, force:Boolean) = Action(parse.json) { request =>
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

  def createKernel = Action { implicit request:RequestHeader =>
    startKernel(UUID.randomUUID.toString)
  }



  def openObservable(contextId:String) =  WebSocket.using[JsValue] { request =>
      import kernelSystem.dispatcher
      //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
      val (out,channel) = Concurrent.broadcast[JsValue]

      val actor = WebSocketObservableActor.props(channel, contextId)

      //log the message to stdout and send response back to client
      val in = Iteratee.foreach[JsValue] { msg =>
        actor ! msg
      }
      (in,out)
  }

  def closeObservable = {
    // TODO ? kernelIdToObsService.remove(contextId)
  }


  def openKernel(kernelId:String, pchannel:String) =  WebSocket.using[JsValue] { request =>
      import kernelSystem.dispatcher

      //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
      val (out,channel) = Concurrent.broadcast[JsValue]

      val actor = WebSocketKernelActor.props(channel, pchannel, kernelIdToCalcService(kernelId))

      //log the message to stdout and send response back to client
      val in = Iteratee.foreach[JsValue] {
        msg => actor ! msg
      }
      (in,out)
  }

  def closeKernel = {
    // TODO ? kernelIdToObsService.remove(contextId)
  }

  def getNotebook(id: String, name: String, format: String) = {
    try {
      val response = nbm.getNotebook(Some(id), name).map { case (lastMod, name, data) =>
        format match {
          case "json" =>
            Ok(Json.parse(data)).withHeaders(
              "Content-Disposition" → s"""attachment; filename="$name.snb" """,
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
    val compilerArgs = config.kernelCompilerArgs.toList
    val initScripts = config.kernelInitScripts.toList
    val kernel = new Kernel(kernelSystem)
    KernelManager.add(kernelId, kernel)

    val service = new CalcWebSocketService(kernelSystem, initScripts, compilerArgs, kernel.remoteDeployFuture)
    kernelIdToCalcService += kernelId -> service

    val json = Json.obj(
      "kernel_id" -> kernelId,
      "ws_url" -> s"ws:/${request.host}"
    )
    Ok(json)
  }

}