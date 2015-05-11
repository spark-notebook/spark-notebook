package controllers

import java.util.UUID
import java.io.File
import java.net.URLDecoder

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._
import play.api.Play.current

import com.typesafe.config._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import notebook._
import notebook.server._
import notebook.kernel.remote._
import notebook.NBSerializer.Metadata

object AppUtils {
  lazy val config               = NotebookConfig(current.configuration.getConfig("manager").get)
  lazy val nbm                  = new NotebookManager(config.projectName, config.notebooksDir)
  lazy val notebookServerConfig = current.configuration.getConfig("notebook-server").get.underlying
  lazy val clustersConf         = config.config.getConfig("clusters").get

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

  val clustersActor = kernelSystem.actorOf(Props(NotebookClusters(AppUtils.clustersConf)))
  implicit val GetClustersTimeout = Timeout(60 seconds)

  val project = "Spark Notebook" //TODO from application.conf
  val base_project_url = current.configuration.getString("application.context").getOrElse("/")
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

  val kernelDef = Json.parse(
        s"""
        |{
        |  "kernelspecs": {
        |    "spark": {
        |      "name": "spark",
        |      "resources": {},
        |      "spec" : {
        |        "language": "scala",
        |        "display_name": "Scala [${notebook.BuildInfo.scalaVersion}] Spark [${notebook.BuildInfo.xSparkVersion}] Hadoop [${notebook.BuildInfo.xHadoopVersion}] ${if (notebook.BuildInfo.xWithHive) " {Hive ✓}" else ""}",
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

  def kernelSpecs() = Action { Ok(kernelDef) }

  private [this] def newSession(kernelId:Option[String]=None, notebookPath:Option[String]=None) = {
    val existing = for {
      path         <- notebookPath
      (id, kernel) <- KernelManager.atPath(path)
    } yield (id, kernel, kernelIdToCalcService(id))

    val (kId, kernel, service) = existing.getOrElse {
      Logger.info(s"Starting kernel/session because nothing for $kernelId and $notebookPath")

      val kId = kernelId.getOrElse(UUID.randomUUID.toString)
      val compilerArgs = config.kernel.compilerArgs.toList
      val initScripts = config.kernel.initScripts.toList
      val kernel = new Kernel(config.kernel.config.underlying, kernelSystem, kId, notebookPath)
      KernelManager.add(kId, kernel)

      val r = Reads.map[String]

      // Load the notebook → get the metadata
      val md:Option[Metadata] = for {
        p <- notebookPath
        n <- nbm.load(p)
        m <- n.metadata
      } yield m

      val customLocalRepo:Option[String] = md.flatMap(_.customLocalRepo)

      val customRepos:Option[List[String]] = md.flatMap(_.customRepos)

      val customDeps:Option[List[String]] = md.flatMap(_.customDeps)

      val customImports:Option[List[String]] = md.flatMap(_.customImports)

      val customSparkConf:Option[Map[String, String]] = for {
        m <- md
        c <- m.customSparkConf
        _ = Logger.info("customSparkConf >> " + c)
        map <- r.reads(c).asOpt
      } yield map

      val service = new CalcWebSocketService( kernelSystem,
                                              customLocalRepo,
                                              customRepos,
                                              customDeps,
                                              customImports,
                                              customSparkConf,
                                              initScripts,
                                              compilerArgs,
                                              kernel.remoteDeployFuture
                                            )
      kernelIdToCalcService += kId -> service
      (kId, kernel, service)
    }

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
    Ok(JsArray(kernelIdToCalcService.keys
      .map { k =>
        KernelManager.get(k).map(l => (k, l))
      }.collect {
        case Some(x) => x
      }.map { case (k, kernel) =>
        val path = kernel.notebookPath.getOrElse(s"KERNEL '$k' SHOULD HAVE A PATH ACTUALLY!")
        Json.obj(
          "notebook" →  Json.obj(
                          "path" → path
                        ),
          "id" →  k
        )
      }.toSeq)
    )
  }


  def profiles() = Action.async {
    implicit val ec = kernelSystem.dispatcher
    (clustersActor ? NotebookClusters.Profiles).map { case all:List[JsObject] =>
      Ok(JsArray(all))
    }
  }

  def clusters() = Action.async {
    implicit val ec = kernelSystem.dispatcher
    (clustersActor ? NotebookClusters.All).map { case all:List[JsObject] =>
      Ok(JsArray(all))
    }
  }

  /**
   *  {
   *    "name": "Med At Scale",
   *    "profile": "Local",
   *    "template": {
   *      "customLocalRepo": "/tmp/spark-notebook/repo",
   *      "customRepos": [],
   *      "customDeps": [
   *        "org.bdgenomics.adam % adam-apis % 0.15.0",
   *        "- org.apache.hadoop % hadoop-client %   _",
   *        "- org.apache.spark  % spark-core    %   _",
   *        "- org.scala-lang    %     _         %   _",
   *        "- org.scoverage     %     _         %   _",
   *        "+ org.apache.spark  %  spark-mllib_2.10  % 1.2.0"
   *      ],
   *      "customImports": [
   *        "import org.apache.hadoop.fs.{FileSystem, Path}",
   *        "import org.bdgenomics.adam.converters.{ VCFLine, VCFLineConverter, VCFLineParser }",
   *        "import org.bdgenomics.formats.avro.{Genotype, FlatGenotype}",
   *        "import org.bdgenomics.adam.models.VariantContext",
   *        "import org.bdgenomics.adam.rdd.ADAMContext._",
   *        "import org.bdgenomics.adam.rdd.variation.VariationContext._",
   *        "import org.bdgenomics.adam.rdd.ADAMContext",
   *        "import org.apache.spark.rdd.RDD"
   *      ],
   *      "customSparkConf": {
   *        "spark.app.name": "Local Adam Analysis",
   *        "spark.master": "local[8]",
   *        "spark.executor.memory": "1G",
   *        "spark.serializer": "org.apache.spark.serializer.KryoSerializer",
   *        "spark.kryo.registrator": "org.bdgenomics.adam.serialization.ADAMKryoRegistrator",
   *        "spark.kryoserializer.buffer.mb": "4",
   *        "spark.kryo.referenceTracking": "true",
   *        "spark.executor.memory": "2g"
   *      }
   *    }
   *  }
   */
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

  def contents(`type`:String, p:String="/") = Action {
    val path = URLDecoder.decode(p)
    val lengthToRoot = config.notebooksDir.getAbsolutePath.size + 1
    val baseDir = new java.io.File(config.notebooksDir, path)

    if (`type` == "directory") {
      val content = Option(baseDir.listFiles.toList)
                      .getOrElse(Nil)
                      .map { f =>
                        val n = f.getName
                        if (f.isFile && n.endsWith(".snb")) {
                          Json.obj(
                            "type" → "notebook",
                            "name" → n.dropRight(".snb".size),
                            "path" → f.getAbsolutePath.drop(lengthToRoot) //todo → build relative path
                          )
                        } else if (f.isFile) {
                          Json.obj(
                            "type" → "file",
                            "name" → n,
                            "path" → f.getAbsolutePath.drop(lengthToRoot) //todo → build relative path
                          )
                        } else {
                          Json.obj(
                            "type" → "directory",
                            "name" → n,
                            "path" → f.getAbsolutePath.drop(lengthToRoot) //todo → build relative path
                          )
                        }
                      }
      Ok(Json.obj(
        "content" → content
      ))
    } else if (`type` == "notebook") {
      Logger.debug("content: " + path)
      val name = if (path.endsWith(".snb")) {path.dropRight(".snb".size)} else {path}
      getNotebook(name, path, "json")
    } else {
      BadRequest("Dunno what to do with contents for " + `type` + "at " + path)
    }
  }

  def createNotebook(p:String, custom:JsObject) = {
    val path = URLDecoder.decode(p)
    Logger.info(s"Creating notebook at $path")
    val customLocalRepo = Try((custom \ "customLocalRepo").as[String]).toOption.map(_.trim()).filterNot(_.isEmpty)
    val customRepos = Try((custom \ "customRepos").as[List[String]]).toOption.filterNot(_.isEmpty)
    val customDeps = Try((custom \ "customDeps").as[List[String]]).toOption.filterNot(_.isEmpty)
    val customImports = Try((custom \ "customImports").as[List[String]]).toOption.filterNot(_.isEmpty)

    val customMetadata = (for {
      j <- Try(custom \ "customSparkConf") if j.isInstanceOf[JsObject]
    } yield j.asInstanceOf[JsObject]).toOption

    val fpath = nbm.newNotebook(
                    path,
                    customLocalRepo,
                    customRepos,
                    customDeps,
                    customImports,
                    customMetadata)
    Try(Redirect(routes.Application.contents("notebook", fpath)))
  }

  def copyingNb(fp:String) = {
    val fromPath = URLDecoder.decode(fp)
    Logger.info("Copying notebook:" + fromPath)
    val np = nbm.copyNotebook(fromPath)
    Try(Ok(Json.obj("path" → np)))
  }

  def newNotebook(path:String, tryJson:Try[JsValue]) = {
    def findkey[T](x:JsValue, k:String)(init:T)(implicit m:ClassTag[T]):Try[T] =
      (x \ k) match {
        case j:JsUndefined => Failure(new IllegalArgumentException("No " + k))
        case JsNull => Success(init)
        case o if m.runtimeClass == o.getClass => Success(o.asInstanceOf[T])
        case x => Failure(new IllegalArgumentException("Bad type: " + x))
      }


    lazy val custom = for {
      x <- tryJson
      t <- findkey[JsObject](x, "custom")(Json.obj())
      n <- createNotebook(path, t)
    } yield n

    lazy val copyFrom = for {
      x <- tryJson
      t <- findkey[JsString](x, "copy_from")(JsString(""))
      n <- copyingNb(t.value)
    } yield n

    (custom orElse copyFrom)
  }

  def newDirectory(path:String) = {
    Logger.info("New dir:" + path)
    val base = new File(AppUtils.config.notebooksDir, path)
    val parent = base.getParentFile()
    val newDir = new File(parent, "dir")
    newDir.mkdirs()
    Try(Ok(Json.obj("path" → (newDir.getAbsolutePath.drop(parent.getAbsolutePath.size)))))
  }

  def newFile(path:String) = {
    Logger.info("New file:" + path)
    val base = new File(AppUtils.config.notebooksDir, path)
    val parent = base.getParentFile()
    val newF = new File(parent, "file")
    newF.createNewFile()
    Try(Ok(Json.obj("path" → (newF.getAbsolutePath.drop(parent.getAbsolutePath.size)))))
  }

  def newContent(p:String="/") = Action(parse.tolerantText) { request =>
    val path = URLDecoder.decode(p)
    val text = request.body
    val tryJson = Try(Json.parse(request.body))

    tryJson.flatMap { json =>
      (json \ "type").as[String] match {
        case "directory" => newDirectory(path)
        case "notebook" => newNotebook(path, tryJson)
        case "file" => newFile(path)
      }
    }.get
  }

  def openNotebook(p:String) = Action { implicit request =>
    val path = URLDecoder.decode(p)
    Logger.info(s"View notebook '$path'")
    val wsPath = base_project_url match {
      case "/" => "/ws"
      case x if x.endsWith("/") => x + "ws"
      case x => x + "/ws"
    }
    val prefix = if (request.secure) "wss" else "ws"
    val ws_url = s"$prefix:/${request.host}$wsPath"

    Ok(views.html.notebook(
      nbm.name,
      Map(
        "base-url" -> base_project_url,
        "ws-url" -> ws_url,

        "base-project-url" -> base_project_url,
        "base-kernel-url" -> base_kernel_url,
        "base-observable-url" -> s"$ws_url/$base_observable_url",
        "read-only" -> read_only,
        "notebook-name" -> nbm.name,
        "notebook-path" -> path,
        "notebook-writable" -> "true"
      )
    ))
  }

  private [this] def closeKernel(kernelId:String) = {
    kernelIdToCalcService -= kernelId

    KernelManager.get(kernelId).foreach{ k =>
      Logger.info(s"Closing kernel $kernelId")
      k.shutdown()
      KernelManager.remove(kernelId)
    }
  }

  def openKernel(kernelId:String, session_id:String) =  ImperativeWebsocket.using[JsValue](
    onOpen = channel => WebSocketKernelActor.props(channel, kernelIdToCalcService(kernelId), session_id),
    onMessage = (msg, ref) => ref ! msg,
    onClose = ref => {
      // try to not close the kernel to allow long live sessions
      // closeKernel(kernelId)
      Logger.info(s"Closing websockets for kernel $kernelId")
      ref ! akka.actor.PoisonPill
    }
  )

  def terminateKernel(kernelId:String) = Action { request =>
    closeKernel(kernelId)
    Ok(s"Kernel $kernelId closed!")
  }

  def restartKernel(kernelId:String) = Action { request =>
    //shouldn't do anything since onClose should be called in openKernel (stopChannels is call in the front)
    // /!\ this won't kill the underneath actor!!!
    closeKernel(kernelId)
    Ok(newSession(notebookPath=KernelManager.get(kernelId).flatMap(k => k.notebookPath)))
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

  def renameNotebook(p:String) = Action(parse.tolerantJson) { request =>
    val path = URLDecoder.decode(p)
    val notebook = (request.body \ "path").as[String]
    Logger.info("RENAME → " + path + " to " + notebook)
    try {
      val (newname, newpath) = nbm.rename(path, notebook)

      Ok(Json.obj(
        "type" → "notebook",
        "name" → newname,
        "path" → newpath
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def saveNotebook(p:String) = Action(parse.tolerantJson(maxLength = AppUtils.config.maxBytesInFlight)) { request =>
    val path = URLDecoder.decode(p)
    Logger.info("SAVE → " + path)
    val notebook = NBSerializer.fromJson(request.body \ "content")
    try {
      val (name, savedPath) = nbm.save(path, notebook, true)

      Ok(Json.obj(
        "type" → "notebook",
        "name" → name,
        "path" → savedPath
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def deleteNotebook(p:String) = Action{ request =>
    val path = URLDecoder.decode(p)
    Logger.info("DELETE → " + path)
    try {
      nbm.deleteNotebook(path)

      Ok(Json.obj(
        "type" → "notebook",
        "path" → path
      ))
    } catch {
      case _ :NotebookExistsException => Conflict
    }
  }

  def dlNotebookAs(p:String, format:String) = Action {
    val path = URLDecoder.decode(p)
    Logger.info("DL → " + path + " as " + format)
    getNotebook(path.dropRight(".snb".size), path, format, true)
  }

  def dash(title:String, p:String=base_kernel_url) = Action {
    val path = URLDecoder.decode(p)
    Logger.debug("DASH → " + path)
    Ok(views.html.projectdashboard(
      title,
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
        path.split("/").toList.scanLeft(("", "")){ case ((accPath, accName), p) => (accPath + "/" + p, p) }.tail.map { case (p, x) =>
          Crumb(controllers.routes.Application.dash(p.tail).url, x)
        }
      )
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

  def getNotebook(name: String, path: String, format: String, dl:Boolean=false) = {
    try {
      Logger.debug(s"getNotebook: name is '$name', path is '$path' and format is '$format'")
      val response = nbm.getNotebook(path).map { case (lastMod, name, data, path) =>
        format match {
          case "json" =>
            val j = Json.parse(data)
            val json = if (!dl) {
              Json.obj(
                "content" → j,
                "name" → name,
                "path" → path, //FIXME
                "writable" -> true //TODO
              )
            } else {
              j
            }
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

  // docker
  val docker /*:Option[tugboat.Docker]*/ = None // SEE dockerlist branch! → still some issues due to tugboat

  def dockerAvailable = Action {
    Ok(Json.obj("available" → docker.isDefined)).withHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Accept, Origin, Content-type",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }

  def dockerList = TODO // SEE dockerlist branch! → still some isues due to tugboat


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
