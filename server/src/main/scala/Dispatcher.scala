package com.bwater.notebook
package server

import akka.remote.RemotingLifecycleEvent
import unfiltered.Cookie
import unfiltered.netty.websockets.{Pass => _, _}
import unfiltered.request._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import akka.actor._
import java.net.URLDecoder
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import com.typesafe.config.ConfigFactory
import client._
import kernel.remote.{Subprocess, AkkaConfigUtils, VMManager}
import com.bwater.notebook.util.Logging
import unfiltered.netty.RequestBinding
import unfiltered.response._
import unfiltered.request.Accepts.Accepting
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

/** unfiltered plan */
class Dispatcher(protected val config: ScalaNotebookConfig,
                 domain: String,
                 port: Int) extends NotebookSession {

  val executionCounter = new AtomicInteger(0)

  val kernelIdToCalcService = collection.mutable.Map[String, CalcWebSocketService]()

  // RH: I have no idea why this isn't part of unfiltered or something... unless it is and my Google-fu failed me.
  object Encoded {
    def unapply(raw: String) = try {
      Some(URLDecoder.decode(raw, "UTF-8"))
    } catch {
      case _: Exception => None
    }
  }


  object WebSockets {
    val intent: unfiltered.netty.websockets.Intent = {
      case req@Path(Seg("kernels" :: kernelId :: channel :: Nil)) => {
        case Open(websock) =>
          for (calcService <- kernelIdToCalcService.get(kernelId)) {
            logInfo("Opening Socket " + channel + " for " + kernelId + " to " + websock)
            if (channel == "iopub")
              calcService.ioPubPromise.success(new WebSockWrapperImpl(websock))
            else if (channel == "shell")
              calcService.shellPromise.success(new WebSockWrapperImpl(websock))
          }
        case Message(socket, Text(msg)) =>
          for (calcService <- kernelIdToCalcService.get(kernelId)) {

            logDebug("Message for " + kernelId + ":" + msg)

            val json = parse(msg)

            for {
              JField("header", header) <- json
              JField("session", session) <- header
              JField("msg_type", msgType) <- header
              JField("content", content) <- json
            } {
              msgType match {
                case JString("execute_request") => {
                  for (JField("code", JString(code)) <- content) {
                    val execCounter = executionCounter.incrementAndGet()
                    calcService.calcActor ! SessionRequest(header, session, ExecuteRequest(execCounter, code))
                  }
                }

                case JString("complete_request") => {
                  for (
                    JField("line", JString(line)) <- content;
                    JField("cursor_pos", JInt(cursorPos)) <- content
                  ) {
                    calcService.calcActor ! SessionRequest(header, session, CompletionRequest(line, cursorPos.toInt))
                  }
                }

                case JString("object_info_request") => {
                  for (JField("oname", JString(oname)) <- content) {
                    calcService.calcActor ! SessionRequest(header, session, ObjectInfoRequest(oname))
                  }
                }

                case x => logWarn("Unrecognized websocket message: " + msg) //throw new IllegalArgumentException("Unrecognized message type " + x)
              }
            }
          }

        case Close(websock) =>
          logInfo("Closing Socket " + websock)
          for (kernel <- KernelManager.get(kernelId)) {
            kernel.shutdown()
          }
        case Error(s, e) =>
          logError("Websocket error", e)
      }
    }
  }

  object WebServer {

    val nbWriteIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@PUT(Path(Seg("notebooks" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val overwrite = req.parameterValues("force").headOption.map(_.toBoolean).getOrElse(false)
        val contents = Body.string(req)
        logDebug("Putting notebook:" + contents)
        val nb = NBSerializer.read(contents)
        try {
          nbm.save(id, name, nb, overwrite)
          PlainTextContent ~> Ok
        } catch {
          case _ :NotebookExistsException => PlainTextContent ~> Conflict
        }

      case req@DELETE(Path(Seg("notebooks" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        try {
          nbm.deleteNotebook(id, name)
          PlainTextContent ~> Ok
        } catch {
          case e: Exception =>
            logError("Error deleting notebook %s".format(name), e)
            InternalServerError
        }
    }

    /* Had to break up notebook functions - see https://issues.scala-lang.org/browse/SI-1133 */
    val nbReadIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path(Seg("notebooks" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val format = req.parameterValues("format").headOption
        getNotebook(id, name, format getOrElse "json")

      case req@POST(Path(Seg("notebooks" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val format = req.parameterValues("format").headOption
        getNotebook(id, name, format getOrElse "json")

      case req@(Path(Seg("new" :: Nil))) =>
        val notebook_id = nbm.newNotebook()
        val notebook_name = nbm.idToName(notebook_id)
        Redirect("/view/" + notebook_name + "?id=" + notebook_id)

      case req@(Path(Seg("copy" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        val newId = nbm.copyNotebook(id, name)
        Redirect("/view/" + nbm.idToName(newId) + "?id=" + newId)
    }

    val otherIntent: unfiltered.netty.cycle.Plan.Intent = {
      case req@GET(Path("/")) =>
        view(req, "projectdashboard.ssp",
          "project" -> nbm.name)

      case GET(Path(Seg("notebooks" :: Nil))) =>
        Json(nbm.listNotebooks)

      case GET(Path(Seg("clusters" :: Nil))) =>
        val s = """[{"profile":"default","status":"stopped","profile_dir":"C:\\Users\\Ken\\.ipython\\profile_default"}]"""
        JsonContent ~> ResponseString(s) ~> Ok

      case req@GET(Path(Seg("view" :: Encoded(name) :: Nil))) =>
        val id = req.parameterValues("id").headOption
        view(req, "notebook.ssp",
          "notebook_id" -> (id getOrElse nbm.notebookId(name)),
          "notebook_name" -> name,
          "project" -> nbm.name,
          "ws_url" ->  "ws:/%s:%d".format(domain, port))


      case req@Path(Seg("print" :: Encoded(name) :: Nil)) =>
        val id = req.parameterValues("id").headOption
        view(req, "printnotebook.ssp",
          "notebook_id" -> (id getOrElse nbm.notebookId(name)),
          "notebook_name" -> name,
          "project" -> nbm.name)

    }

    def getNotebook(id: Option[String], name: String, format: String) = {
      try {
        val response = for ((lastMod, name, data) <- nbm.getNotebook(id, name))
        yield format match {
            case "json" => JsonContent ~> ResponseHeader(
                                            "Content-Disposition", 
                                            "attachment; filename=\"%s.snb".format(name) :: Nil
                                          ) ~> ResponseHeader("Last-Modified", lastMod :: Nil) ~> ResponseString(data) ~> Ok

            case _ => PlainTextContent ~> ResponseString("Unsupported format.") ~> NotFound
          }
        response getOrElse PlainTextContent ~> ResponseString("Notebook not found.") ~> NotFound
      } catch {
        case e: Exception =>
          logError("Error accessing notebook %s".format(name), e)
          InternalServerError
      }
    }


    def startKernel(kernelId: String) = {
      val compilerArgs = config.kernelCompilerArgs
      val initScripts = config.kernelInitScripts
      val kernel = new Kernel(system)
      KernelManager.add(kernelId, kernel)
      val service = new CalcWebSocketService(system, initScripts, compilerArgs, kernel.remoteDeployFuture)
      kernelIdToCalcService += kernelId -> service
      val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws:/%s:%d".format(domain, port))
      JsonContent ~> ResponseString(compact(render(json))) ~> Ok
    }

    def getCalcWebSocketService(kernelId: String):Option[CalcWebSocketService] = kernelIdToCalcService.get(kernelId)

    val kernelIntent: unfiltered.netty.async.Plan.Intent = {
      case req@POST(Path(Seg("kernels" :: Nil))) =>
        logInfo("Starting kernel")
        req.respond(startKernel(UUID.randomUUID.toString))

      case req@POST(Path(Seg("kernels" :: kernelId :: "restart" :: Nil))) =>
        logInfo("Restarting kernel " + kernelId)
        for (kernel <- KernelManager.get(kernelId)) {
          kernel.router ! RestartKernel
        }
        val json = ("kernel_id" -> kernelId) ~ ("ws_url" -> "ws:/%s:%d".format(domain, port))
        val resp = JsonContent ~> ResponseString(compact(render(json))) ~> Ok
        req.respond(resp)

      case req@POST(Path(Seg("kernels" :: kernelId :: "interrupt" :: Nil))) =>
        logInfo("Interrupting kernel " + kernelId)
        for (calcService <- kernelIdToCalcService.get(kernelId)) {
          calcService.calcActor ! InterruptCalculator
        }
        req.respond(PlainTextContent ~> Ok)
    }

    def view[T](req: HttpRequest[T], file: String, extra: (String, Any)*) = {
      val Params(params) = req
      // Workaround for browsers that don't accept XHTML (e.g. older versions of IE)
      val contentType = req match {
        case XhtmlReq(r) => XhtmlContent
        case _ => HtmlContent
      }
      contentType ~> Scalate(req, "templates/" + file, (params.toSeq ++ extra): _*)
    }
  }

  object XhtmlReq extends Accepting {
    val contentType = "application/xhtml+xml"
    val ext = "xhtml"
  }
  object XhtmlContent extends CharContentType("application/xhtml+xml")
}


trait DispatcherSecurity {
  def loginPath: String
  def authIntent: unfiltered.netty.cycle.Plan.Intent
  def withCSRFKeyAsync(asyncIntent: unfiltered.netty.async.Plan.Intent): unfiltered.netty.async.Plan.Intent
  def withCSRFKey(intent: unfiltered.netty.cycle.Plan.Intent): unfiltered.netty.cycle.Plan.Intent
  def withWSAuth(wsIntent: unfiltered.netty.websockets.Intent): unfiltered.netty.websockets.Intent
}

object Insecure extends DispatcherSecurity {
  val loginPath = ""
  val authIntent: unfiltered.netty.cycle.Plan.Intent = { case x => Pass }
  def withCSRFKeyAsync(asyncIntent: unfiltered.netty.async.Plan.Intent) = asyncIntent
  def withCSRFKey(intent: unfiltered.netty.cycle.Plan.Intent) = intent
  def withWSAuth(wsIntent: unfiltered.netty.websockets.Intent) = wsIntent
}

class ClientAuth(domain: String, port: Int) extends Logging with DispatcherSecurity {
  val loginToken = akka.util.Crypt.generateSecureCookie // TODO: Placeholder for more robust client authentication scheme
  val csrfKey = akka.util.Crypt.generateSecureCookie
  val sessionKey = akka.util.Crypt.generateSecureCookie
  private val loginTokenValid = new AtomicBoolean(true)

  val sessID_tag = "sessID" + port
  val csrfKey_tag = "CSRF-Key"

  private val sessC = Cookie(sessID_tag, sessionKey, httpOnly = true, path = Some("/"), domain = Some(domain))
  private val csrfC = Cookie(csrfKey_tag, csrfKey, path = Some("/"), domain = Some(domain))

  val loginPath = "login/%s".format(loginToken)

  override val authIntent: unfiltered.netty.cycle.Plan.Intent = {
    case req@GET(Path(Seg("login" :: `loginToken` :: Nil))) if (loginTokenValid.get) =>
      loginTokenValid.set(false) //Can't use 'getAndSet' above, it is executed before an actual match...
      val Params(params) = req
      val dest = for {
        ds <- params.get("dest")
        d <- ds.headOption
      } yield d
      SetCookies(
        sessC,
        csrfC) ~> Redirect(dest.getOrElse("/"))
    case req@GET(Path(Seg("login" :: token :: Nil))) =>
      PlainTextContent ~> Forbidden ~> ResponseString("Unauthorized")
    case req & Cookies(cookies) =>
      logTrace("Cookies: " + cookies)
      cookies(sessID_tag).map(_.value) match {
        case Some(`sessionKey`) => Pass
        case _ => PlainTextContent ~> Forbidden ~> ResponseString("Unauthorized")
      }
  }

  override def withCSRFKeyAsync(asyncIntent: unfiltered.netty.async.Plan.Intent): unfiltered.netty.async.Plan.Intent = {
    case req if (asyncIntent isDefinedAt (req)) =>
      if (req.headers(csrfKey_tag).contains(csrfKey) || req.method.equalsIgnoreCase("POST") && req.parameterValues(csrfKey_tag).contains(csrfKey))
        asyncIntent(req)
      else req.respond(PlainTextContent ~> Forbidden ~> ResponseString("Missing key in request."))
  }

  override def withCSRFKey(intent: unfiltered.netty.cycle.Plan.Intent): unfiltered.netty.cycle.Plan.Intent = {
    case req if (intent.isDefinedAt(req)) =>
      if (req.headers(csrfKey_tag).contains(csrfKey) || req.method.equalsIgnoreCase("POST") && req.parameterValues(csrfKey_tag).contains(csrfKey))
        intent(req) //SetCookies(csrfC) ~> intent(req)
      else PlainTextContent ~> Forbidden ~> ResponseString("Missing key in request.")
  }

  /* Wraps a websocket intent, checks for session key iff the wrapped intent can handle the request */
  override def withWSAuth(wsIntent: unfiltered.netty.websockets.Intent): unfiltered.netty.websockets.Intent = {
    case (req: RequestBinding) & Cookies(cookies) if (wsIntent.isDefinedAt(req)) =>
      logTrace("Cookies: " + cookies)
      cookies(sessID_tag).map(_.value) match {
        case Some(`sessionKey`) => wsIntent(req)
        case _ => req.respond(PlainTextContent ~> Forbidden ~> ResponseString("Unauthorized")); Map.empty
      }
  }

}


trait NotebookSession extends Logging {
  protected def config: ScalaNotebookConfig

  val nbm = new NotebookManager(config.projectName, config.notebooksDir)
  val system = ActorSystem( "NotebookServer", 
                            AkkaConfigUtils.optSecureCookie(
                              ConfigFactory.load("notebook-server"), 
                              akka.util.Crypt.generateSecureCookie
                            )
                          )
  logInfo("Notebook session initialized")

  ifDebugEnabled {
    system.eventStream.subscribe(system.actorOf(Props(new Actor {
      def receive = {
        case x => logDebug("Actor Lifecycle event: " + x)
      }
    })), classOf[RemotingLifecycleEvent ])
  }

}
