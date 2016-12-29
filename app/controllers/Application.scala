package controllers

import java.io.File
import java.net.URLDecoder
import java.util.UUID

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import notebook.NBSerializer.Metadata
import notebook._
import notebook.server._
import play.api.Play.current
import play.api._
import play.api.http.HeaderNames
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.mvc._
import utils.AppUtils
import utils.Const.UTF_8

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

case class Crumb(url: String = "", name: String = "")

case class Breadcrumbs(home: String = "/", crumbs: List[Crumb] = Nil)


object Application extends Controller {

  private lazy val config = AppUtils.config
  private lazy val notebookManager = AppUtils.notebookManager
  private val kernelIdToCalcService = collection.mutable.Map[String, CalcWebSocketService]()
  private val kernelIdToObservableActor = collection.mutable.Map[String, ActorRef]()
  private val clustersActor = kernelSystem.actorOf(Props(NotebookClusters(AppUtils.clustersConf)))

  private implicit def kernelSystem: ActorSystem = AppUtils.kernelSystem

  private implicit val GetClustersTimeout = Timeout(60 seconds)

  val project = notebookManager.name
  val base_project_url = current.configuration.getString("application.context").getOrElse("/")
  val autoStartKernel = current.configuration.getBoolean("manager.kernel.autostartOnNotebookOpen").getOrElse(true)
  val kernelKillTimeout = current.configuration.getMilliseconds("manager.kernel.killTimeout")
  val base_kernel_url = "/"
  val base_observable_url = "observable"
  val read_only = false.toString
  //  TODO: Ugh...
  val terminals_available = false.toString // TODO

  def configTree() = Action {
    Ok(Json.obj())
  }

  def configCommon() = Action {
    Ok(Json.obj())
  }

  def configNotebook() = Action {
    Ok(Json.obj())
  }

  private val kernelDef = Json.parse(
    s"""
    |{
    |  "kernelspecs": {
    |    "spark": {
    |      "name": "spark",
    |      "resources": {},
    |      "spec" : {
    |        "language": "scala",
    |        "display_name": "Scala [${notebook.BuildInfo.scalaVersion}] Spark [${notebook.BuildInfo.xSparkVersion}] Hadoop [${notebook.BuildInfo.xHadoopVersion}] ${if (notebook.BuildInfo.xWithHive) " {Hive ✓}" else ""} ${if (notebook.BuildInfo.xWithParquet) " {Parquet ✓}" else ""}",
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

  def kernelSpecs() = Action {
    Ok(kernelDef)
  }

  // this function takes two optional lists and add the values in the second one
  // the values are considered to be unique
  private def overrideOptionListDistinctString(x:Option[List[String]], y:Option[List[String]]) = {
    val t = x.getOrElse(List.empty[String])
    val withY =  t ::: (y.getOrElse(List.empty[String]).toSet -- t.toSet).toList
    withY match {
      case Nil => None
      case xs => Some(xs.distinct)
    }
  }

  private[this] def kernelResponse(id:Option[String]) =
    Json.parse(
      s"""
         |{
         |"id": ${id.map((i) => "\""+i+"\"").getOrElse("null")},
         |"name": "spark",
         |"language_info": {
         |  "name" : "Scala",
         |  "file_extension" : "scala",
         |  "codemirror_mode" : "text/x-scala"
         |}
         |}
         |""".stripMargin.trim
    )

  def getSession(notebookPath:String) = Action {
    val id = KernelManager.atPath(notebookPath).map(_._1)
    Ok(Json.obj("kernel" → kernelResponse(id)))
  }

  private[this] def newSession(kernelId: Option[String] = None, notebookPath: Option[String] = None) = {
    val existing = for {
      path <- notebookPath
      (id, kernel) <- KernelManager.atPath(path)
    } yield (id, kernel, kernelIdToCalcService(id))

    val (kId, kernel, service) = existing.getOrElse {
      Logger.info(s"Starting kernel/session because nothing for $kernelId and $notebookPath")

      val kId = kernelId.getOrElse(UUID.randomUUID.toString)
      val compilerArgs = config.kernel.compilerArgs.toList
      val initScripts = config.kernel.initScripts.toList

      // Load the notebook → get the metadata
      val md: Option[Metadata] = for {
        p <- notebookPath
        n <- notebookManager.load(p)
        m <- n.metadata
      } yield m

      val customLocalRepo: Option[String] =
        md.flatMap(_.customLocalRepo) // get the custom in the metadata
          .map { l =>
            config.overrideConf.localRepo // precedence on the override
                               .getOrElse(l) // get back to custom if exists
          }
          .orElse(config.overrideConf.localRepo) // if no custom, try fallback on override

      val customRepos: Option[List[String]] =
        overrideOptionListDistinctString(
          md.flatMap(_.customRepos),
          config.overrideConf.repos
        )

      val customDeps: Option[List[String]] =
        overrideOptionListDistinctString(
          md.flatMap(_.customDeps),
          config.overrideConf.deps
        )


      val customImports: Option[List[String]] =
        overrideOptionListDistinctString(
          md.flatMap(_.customImports),
          config.overrideConf.imports
        )

      val customArgs: Option[List[String]] =
        overrideOptionListDistinctString(
          md.flatMap(_.customArgs),
          config.overrideConf.args
        ).map(xs => xs map notebook.util.StringUtils.updateWithVarEnv)

      val customSparkConf: Option[Map[String, String]] = {
        val me = Map.empty[String, String]
        val sconf = for {
                      m <- md
                      c <- m.customSparkConf
                      v <- CustomConf.fromSparkConfJsonToMap(c)
                    } yield v
        val ovsc = config.overrideConf.sparkConf flatMap CustomConf.fromSparkConfJsonToMap
        val updated = sconf.getOrElse(me) ++ ovsc.getOrElse(me)
        if (updated.isEmpty) {
          None
        } else {
          Some(updated.map{ case (k, v) => k → notebook.util.StringUtils.updateWithVarEnv(v) })
        }
      }

      val kernel = new Kernel(config.kernel.config.underlying,
                              kernelSystem,
                              kId,
                              notebookPath,
                              Some(
                                customArgs.getOrElse(List.empty[String]) :::
                                AppUtils.proxy.all.map{ case (k,v) => s"""-D$k=$v"""}
                              ))
      KernelManager.add(kId, kernel)

      val service = new CalcWebSocketService(kernelSystem,
        appNameToDisplay(md, notebookPath),
        customLocalRepo,
        customRepos,
        customDeps,
        customImports,
        customArgs,
        customSparkConf,
        initScripts,
        compilerArgs,
        kernel,
        kernelTimeout = kernelKillTimeout
      )
      kernelIdToCalcService += kId -> service
      (kId, kernel, service)
    }

    // todo add MD?
    kernelResponse(Some(kId))
  }

  def createSession() = Action(parse.tolerantJson) /* → posted as urlencoded form oO */ { request =>
    val json: JsValue = request.body
    val kernelId = Try((json \ "kernel" \ "id").as[String]).toOption
    val notebookPath = Try((json \ "notebook" \ "path").as[String]).toOption
    val k = newSession(kernelId, notebookPath)
    Ok(Json.obj("kernel" → k))
  }

  def sessions() = Action {
    Ok(JsArray(kernelIdToCalcService.keys
      .map { k =>
      KernelManager.get(k).map(l => (k, l))
    }.collect {
      case Some(x) => x
    }.map { case (k, kernel) =>
      val path: String = kernel.notebookPath.getOrElse(s"KERNEL '$k' SHOULD HAVE A PATH ACTUALLY!")
      Json.obj(
        "notebook" → Json.obj("path" → path),
        "id" → k
      )
    }.toSeq)
    )
  }


  def profiles() = Action.async {
    implicit val ec = kernelSystem.dispatcher
    (clustersActor ? NotebookClusters.Profiles).map { case all: List[JsObject] =>
      Ok(JsArray(all))
    }
  }

  def clusters() = Action.async {
    implicit val ec = kernelSystem.dispatcher
    (clustersActor ? NotebookClusters.All).map { case all: List[JsObject] =>
      Ok(JsArray(all))
    }
  }

  /**
   * add a spark cluster by json meta
   */
  def addCluster() = Action.async(parse.tolerantJson) { request =>
    val json = request.body
    implicit val ec = kernelSystem.dispatcher
    json match {
      case o: JsObject =>
        (clustersActor ? NotebookClusters.Add((json \ "name").as[String], o)).map { case cluster: JsObject =>
          Ok(cluster)
        }
      case _ => Future {
        BadRequest("Add cluster needs an object, got: " + json)
      }
    }
  }
  /**
   * add a spark cluster by json meta
   */
  def deleteCluster(clusterName:String) = Action.async { request =>
      Logger.debug("Delete a cluster")
      implicit val ec = kernelSystem.dispatcher
      (clustersActor ? NotebookClusters.Remove(clusterName, null)).map{ item => Ok(Json.obj("result" → s"Cluster $clusterName deleted"))}
  }

  def contents(tpe: String, uri: String = "/") = Action { request =>
    val path = URLDecoder.decode(uri, UTF_8)
    val lengthToRoot = config.notebooksDir.getAbsolutePath.length
    def dropRoot(f: java.io.File) = f.getAbsolutePath.drop(lengthToRoot).dropWhile(_ == '/')
    val baseDir = new java.io.File(config.notebooksDir, path)

    if (tpe == "directory") {
      val content = Option(baseDir.listFiles).getOrElse(Array.empty).map { f =>
        val n = f.getName
        if (f.isFile && n.endsWith(".snb")) {
          Json.obj(
            "type" -> "notebook",
            "name" -> n.dropRight(".snb".length),
            "path" -> dropRoot(f) //todo → build relative path
          )
        } else if (f.isFile) {
          Json.obj(
            "type" -> "file",
            "name" -> n,
            "path" -> dropRoot(f) //todo → build relative path
          )
        } else {
          Json.obj(
            "type" -> "directory",
            "name" -> n,
            "path" -> dropRoot(f) //todo → build relative path
          )
        }
      }
      Ok(Json.obj("content" → content))
    } else if (tpe == "notebook") {
      Logger.debug("content: " + path)
      val name = if (path.endsWith(".snb")) path.dropRight(".snb".length) else path
      getNotebook(name, path, "json")
    } else {
      BadRequest("Dunno what to do with contents for " + tpe + "at " + path)
    }
  }

  def createNotebook(p: String, custom: JsObject, name:Option[String]) = {
    val path = URLDecoder.decode(p, UTF_8)
    Logger.info(s"Creating notebook at $path")
    val customLocalRepo = Try((custom \ "customLocalRepo").as[String]).toOption.map(_.trim()).filterNot(_.isEmpty)
    val customRepos = Try((custom \ "customRepos").as[List[String]]).toOption.filterNot(_.isEmpty)
    val customDeps = Try((custom \ "customDeps").as[List[String]]).toOption.filterNot(_.isEmpty)
    val customImports = Try((custom \ "customImports").as[List[String]]).toOption.filterNot(_.isEmpty)
    val customArgs = Try((custom \ "customArgs").as[List[String]]).toOption.filterNot(_.isEmpty)

    val customMetadata = (for {
      j <- Try(custom \ "customSparkConf") if j.isInstanceOf[JsObject]
    } yield j.asInstanceOf[JsObject]).toOption

    val fpath = notebookManager.newNotebook(
      path,
      customLocalRepo orElse config.customConf.localRepo,
      customRepos orElse config.customConf.repos,
      customDeps orElse config.customConf.deps,
      customImports orElse config.customConf.imports,
      customArgs orElse config.customConf.args,
      customMetadata orElse config.customConf.sparkConf,
      name)
    Try(Redirect(routes.Application.contents("notebook", fpath)))
  }

  def copyingNb(fp: String) = {
    val fromPath = URLDecoder.decode(fp, UTF_8)
    Logger.info("Copying notebook:" + fromPath)
    val np = notebookManager.copyNotebook(fromPath)
    Try(Ok(Json.obj("path" → np)))
  }

  def newNotebook(path: String, tryJson: Try[JsValue]) = {
    def findkey[T](x: JsValue, k: String)(init: Option[T])(implicit m: ClassTag[T]): Try[T] =
      (x \ k) match {
        case j: JsUndefined => Failure(new IllegalArgumentException("No " + k))
        case JsNull => init.map(x => Success(x)).getOrElse(Failure(new IllegalStateException("Got JsNull ")))
        case o if m.runtimeClass == o.getClass => Success(o.asInstanceOf[T])
        case x => Failure(new IllegalArgumentException("Bad type: " + x))
      }

    lazy val custom = for {
      x <- tryJson
      t <- findkey[JsObject](x, "custom")(Some(Json.obj()))
      n <- createNotebook(path, t, findkey[JsString](x, "name")(None).toOption.map(_.value))
    } yield n

    lazy val copyFrom = for {
      x <- tryJson
      t <- findkey[JsString](x, "copy_from")(Some(JsString("")))
      n <- copyingNb(t.value)
    } yield n

    custom orElse copyFrom
  }

  def newDirectory(path: String, name:String) = {
    Logger.info("New dir: " + path)
    val base = new File(config.notebooksDir, path)
    val parent = base
    val newDir = new File(parent, name)
    newDir.mkdirs()
    Try(Ok(Json.obj("path" → newDir.getAbsolutePath.drop(parent.getAbsolutePath.length))))
  }

  def newFile(path: String) = {
    Logger.info("New file:" + path)
    val base = new File(config.notebooksDir, path)
    val parent = base.getParentFile
    val newF = new File(parent, "file")
    newF.createNewFile()
    Try(Ok(Json.obj("path" → newF.getAbsolutePath.drop(parent.getAbsolutePath.length))))
  }

  def newContent(p: String = "/") = Action(parse.tolerantText) { request =>
    val path = URLDecoder.decode(p, UTF_8)
    val text = request.body
    val tryJson = Try(Json.parse(request.body))

    tryJson.flatMap { json =>
      (json \ "type").as[String] match {
        case "directory" => newDirectory(path, (json \ "name").as[String])
        case "notebook" => newNotebook(path, tryJson)
        case "file" => newFile(path)
      }
    }.get
  }

  def openNotebook(p: String, presentation: Option[String]) = Action { implicit request =>
    val path = URLDecoder.decode(p, UTF_8)
    Logger.info(s"View notebook '$path', presentation: '$presentation'")
    val wsPath = base_project_url match {
      case "/" => "/ws"
      case x if x.endsWith("/") => x + "ws"
      case x => x + "/ws"
    }
    def ws_url(path: Option[String] = None) = {
      s"""
         |window.notebookWsUrl = function() {
         |return ((window.location.protocol=='https:') ? 'wss' : 'ws')+'://'+window.location.host+'$wsPath${path.map(x => "/" + x).getOrElse("")}'
         |};
      """.stripMargin.replaceAll("\n", " ")
    }

    Ok(views.html.notebook(
      project + ":" + path,
      project,
      Map(
        "base-url" -> base_project_url,
        "ws-url" -> ws_url(),
        "base-project-url" -> base_project_url,
        "base-kernel-url" -> base_kernel_url,
        "base-observable-url" -> ws_url(Some(base_observable_url)),
        "read-only" -> read_only,
        "notebook-name" -> notebookManager.name,
        "notebook-path" -> path,
        "notebook-writable" -> "true",
        "presentation" -> presentation.getOrElse("edit")
      ),
      Some("notebook")
    ))
  }

  private[this] def closeKernel(kernelId: String) = {
    kernelIdToCalcService -= kernelId

    KernelManager.get(kernelId).foreach { k =>
      Logger.info(s"Closing kernel $kernelId")
      k.shutdown()
    }
  }

  def openKernel(kernelId: String, sessionId: String) = ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketKernelActor.props(channel, kernelIdToCalcService(kernelId), sessionId),
    onMessage = (msg, ref) => ref ! msg,
    onClose = (channel, ref) => {
      // try to not close the kernel to allow long live sessions
      // closeKernel(kernelId)
      Logger.info(s"Closing websockets for kernel $kernelId")
      ref ! akka.actor.PoisonPill
    }
  )

  def terminateKernel(kernelId: String) = Action { request =>
    closeKernel(kernelId)
    Ok(s"""{"$kernelId": "closed"}""")
  }

  def restartKernel(kernelId: String) = Action(parse.tolerantJson) { request =>
    val k = KernelManager.get(kernelId)
    closeKernel(kernelId)
    val p = (request.body \ "notebook_path").as[String]
    val path = URLDecoder.decode(p, UTF_8)
    val notebookPath = k.flatMap(_.notebookPath).getOrElse(p)
    Ok(newSession(notebookPath = Some(notebookPath)))
  }

  def listCheckpoints(snb: String) = Action { request =>
    Ok(Json.parse(
      """
        |[
        | { "id": "TODO", "last_modified": "2015-01-02T13:22:01.751Z" }
        |]
        | """.stripMargin.trim
    ))
  }

  def saveCheckpoint(snb: String) = Action { request =>
    //TODO
    Ok(Json.parse(
      """
        |[
        | { "id": "TODO", "last_modified": "2015-01-02T13:22:01.751Z" }
        |]
        | """.stripMargin.trim
    ))
  }

  def renameNotebook(p: String) = Action(parse.tolerantJson) { request =>
    val oldPath = URLDecoder.decode(p, UTF_8)
    val newPath = (request.body \ "path").as[String]
    Logger.info("RENAME → " + oldPath + " to " + newPath)
    try {
      val (newname, newpath) = notebookManager.rename(oldPath, newPath)

      KernelManager.atPath(oldPath).foreach { case (_, kernel) =>
        kernel.moveNotebook(newpath)
      }

      Ok(Json.obj(
        "type" → "notebook",
        "name" → newname,
        "path" → newpath
      ))
    } catch {
      case _: NotebookExistsException => Conflict
    }
  }

  def saveNotebook(p: String) = Action(parse.tolerantJson(config.maxBytesInFlight)) { request =>
    val path = URLDecoder.decode(p, UTF_8)
    Logger.info("SAVE → " + path)

    Try {
      val notebookJsObject = (request.body \ "content").asInstanceOf[JsObject]
      NBSerializer.fromJson(notebookJsObject) match {
        case Some(notebook) =>
          Try {
            val (name, savedPath) = notebookManager.save(path, notebook, overwrite = true)
            Ok(Json.obj(
              "type" → "notebook",
              "name" → name,
              "path" → savedPath
            ))
          } recover {
            case _: NotebookExistsException => Conflict
            case anyOther: Throwable =>
              Logger.error(anyOther.getMessage)
              InternalServerError
          } getOrElse {
            InternalServerError
          }
        case None =>
          BadRequest("Not a valid notebook.")
      }
    }.recover {
      case e:ClassCastException =>
        BadRequest("Not a notebook.")
      case anyOther: Throwable =>
        Logger.error(anyOther.getMessage)
        InternalServerError
    } getOrElse {
      InternalServerError
    }
  }

  def deleteNotebook(p: String) = Action { request =>
    val path = URLDecoder.decode(p, UTF_8)
    Logger.info("DELETE → " + path)
    try {
      notebookManager.deleteNotebook(path)

      Ok(Json.obj(
        "type" → "notebook",
        "path" → path
      ))
    } catch {
      case _: NotebookExistsException => Conflict
    }
  }

  def dlNotebookAs(p: String, format: String) = Action {
    val path = URLDecoder.decode(p, UTF_8)
    Logger.info("DL → " + path + " as " + format)
    getNotebook(path.dropRight(".snb".length), path, format, dl = true)
  }

  def dash(p: String = base_kernel_url) = Action {
    val path = URLDecoder.decode(p, UTF_8)
    Logger.debug("DASH → " + path)
    Ok(views.html.projectdashboard(
      notebookManager.name,
      project,
      Map(
        "project" → project,
        "base-project-url" → base_project_url,
        "base-kernel-url" → base_kernel_url,
        "read-only" → read_only,
        "base-url" → base_project_url,
        "notebook-path" → path,
        "terminals-available" → terminals_available
      ),
      Breadcrumbs(
        "/",
        path.split("/").toList.scanLeft(("", "")) {
          case ((accPath, accName), p) => (accPath + "/" + p, p)
        }.drop(1).map { case (p, x) =>
          Crumb(controllers.routes.Application.dash(p.tail).url, x)
        }
      ),
      Some("dashboard")
    ))
  }

  def openObservable(contextId: String) = ImperativeWebsocket.using[JsValue](
    onOpen = channel => {
      kernelIdToObservableActor.get(contextId) match {
        case None =>
          val a = WebSocketObservableActor.props(channel, contextId)
          kernelIdToObservableActor += contextId → a
          a
        case Some(a) =>
          a ! ("add", channel)
          a
      }
    },
    onMessage = (msg, ref) => ref ! msg,
    onClose = (channel, ref) => {
      Logger.info(s"Closing observable sockect $channel for $contextId")
      ref ! ("remove", channel)
    }
  )

  /**
    * The notebook name to attach to Spark Context (and all related jobs)
    */
  def appNameToDisplay(metadata: Option[Metadata], notebookPath: Option[String]): String = {
    val explicitName = metadata.map(_.name).getOrElse("Spark notebook")
    notebookPath match {
      case Some(path) => path
      case None => explicitName
    }
  }

  def getNotebook(name: String, path: String, format: String, dl: Boolean = false) = {
    try {
      Logger.debug(s"getNotebook: name is '$name', path is '$path' and format is '$format'")
      val response = notebookManager.getNotebook(path).map { case (lastMod, nbname, data, fpath) =>
        format match {
          case "json" =>
            val j = Json.parse(data)
            val json = if (!dl) {
              Json.obj(
                "content" → j,
                "name" → nbname,
                "path" → fpath, //FIXME
                "autoStartKernel" -> autoStartKernel,
                "writable" -> true //TODO
              )
            } else {
              j
            }
            Ok(json).withHeaders(
              HeaderNames.CONTENT_DISPOSITION → s"""attachment; filename="$path" """,
              HeaderNames.CONTENT_ENCODING → "UTF-8",
              HeaderNames.LAST_MODIFIED → lastMod
            )
          case "scala" =>
            NBSerializer.fromJson(Json.parse(data)) match {
              case Some(nb) =>
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
                }.getOrElse("//NO CELLS!")

                Ok(code).withHeaders(
                  HeaderNames.CONTENT_DISPOSITION → s"""attachment; filename="$name.scala" """,
                  HeaderNames.LAST_MODIFIED → lastMod
                )
              case None =>
                InternalServerError(s"Notebook could not be parsed.")
            }
          case "markdown" =>
            NBSerializer.fromJson(Json.parse(data)) match {
              case Some(nb) =>
                notebook.export.Markdown.generate(nb, name, false) match {
                  case Some(Left(code)) =>  
                    Ok(code).withHeaders(
                      HeaderNames.CONTENT_DISPOSITION → s"""attachment; filename="$name.md" """,
                      HeaderNames.LAST_MODIFIED → lastMod
                    )
                  case Some(Right(file)) => 
                    Ok.sendFile(
                      content = file,
                      fileName = _ => name+".zip"
                    )
                  case _ => BadRequest(s"No Cells!")
                }
              case None =>
                InternalServerError(s"Notebook could not be parsed.")
            }
          case _ => InternalServerError(s"Unsupported format $format")
        }
      }

      response getOrElse NotFound(s"Notebook '$name' not found at $path.")
    } catch {
      case e: Exception =>
        Logger.error("Error accessing notebook [%s]".format(name), e)
        InternalServerError
    }
  }

  // docker
  val docker /*:Option[tugboat.Docker]*/ = None // SEE dockerlist branch! → still some issues due to tugboat

  def dockerAvailable = Action {
    Ok(Json.obj("available" → docker.isDefined)).withHeaders(
      HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
      HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "GET, POST, PUT, DELETE, OPTIONS",
      HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "Accept, Origin, Content-type",
      HeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
    )
  }

  def dockerList = TODO

  // SEE dockerlist branch! → still some isues due to tugboat
  // util
  object ImperativeWebsocket {

    def using[E: WebSocket.FrameFormatter](
      onOpen: Channel[E] => ActorRef,
      onMessage: (E, ActorRef) => Unit,
      onClose: (Channel[E], ActorRef) => Unit,
      onError: (String, Input[E]) => Unit = (e: String, _: Input[E]) => Logger.error(e)
    ): WebSocket[E, E] = {
      implicit val sys = kernelSystem.dispatcher

      val promiseIn = Promise[Iteratee[E, Unit]]()

      val out = Concurrent.unicast[E](
        onStart = channel => {
          val ref = onOpen(channel)
          val in = Iteratee.foreach[E] { message =>
            onMessage(message, ref)
          } map (_ => onClose(channel, ref))
          promiseIn.success(in)
        },
        onError = onError
      )

      WebSocket.using[E](_ => (Iteratee.flatten(promiseIn.future), out))
    }
  }

}
