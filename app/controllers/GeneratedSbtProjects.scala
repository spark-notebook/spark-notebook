package controllers

import java.io.File
import java.net.URLDecoder
import java.nio.file.Paths

import akka.actor._
import com.datafellas.g3nerator.model._
import notebook.io.ConfigurationMissingException
import notebook.server.NotebookConfig
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.api.mvc._
import utils.ConfigurationUtils._
import utils.Const.UTF_8
import utils.{SbtProjectGenUtils, AppUtils}

import scala.language.postfixOps
import scala.util.{Failure, Try}

object GeneratedSbtProjects extends Controller {
  private implicit def kernelSystem: ActorSystem = AppUtils.kernelSystem

  val baseConfig = current.configuration
  //val notebookConfig = NotebookConfig(baseConfig.getMandatoryConfig("manager"))

  private def projectManager = SbtProjectGenUtils.projectManager.get
  private def projectStore = SbtProjectGenUtils.projectStore.get

  def logException[T](key:String):PartialFunction[Throwable, Try[T]] = {
    case ex: Exception =>
      Logger.error(s"Could not load config [$key]. Reason: [${ex.getMessage}]")
      Failure(ex)
  }

  def sourcesFileName = "sources.zip"

  def gitWebRootHttps: Option[String] = {
    SbtProjectGenUtils.config.config.getString("git_www")
  }

  def readBuildStatus(outputDir: File) = {
    val file = new File(outputDir, "compile.statuscode")
    file match {
      case f if f.exists() =>
        val status = fileContent(f, from = 0).mkString
        status match {
          case "0" => "success"
          case _ => "danger"
        }
      case _ => "warning" // not exists
    }
  }

  def projects() = Action {
    val arr = projectStore.list.map { generatedProject =>
      val outputDir = (new File(projectManager.projectDir, generatedProject.outputDirectory.get)).toPath.toRealPath().toFile
      val projectName = generatedProject.name.get

      // FIXME coursier val downloadUrl: String = controllers.routes.GeneratedSbtProjects.downloadFile(projectName, sourcesFileName).url
      val downloadUrl: String = s"/projects/$projectName/zip/$sourcesFileName"

      val logO: Option[Array[String]] = for {
        jobOut <- at(outputDir, "out")
      } yield fileContent(jobOut, from = 0)

      Json.obj(
        "project" → generatedProject.outputDirectory,
        "version" → generatedProject.version,
        "job" → outputDir.exists,
        "zip_archive_url" →  getProjectFile(projectName, sourcesFileName).toOption.map(_ => downloadUrl),
        "git_repo_https_dir" →  gitWebRootHttps.map(_ + "/" + projectName),
        "build_status" -> readBuildStatus(outputDir),
        "services" → Seq[String]()
      )
    }
    Ok(Json.toJson(arr))
  }

  private def fileContent(f:File, from:Int) = scala.io.Source.fromFile(f).getLines.drop(from).toArray

  private def at(p:String, c:String):Option[File] = at(new File(p), c)

  private def at(p:File, c:String):Option[File] = {
    val f = new File(p, c)
    if (f.exists) Some(f) else None
  }

  def fetchProject(projectName: String): Option[ProjectConfig] = {
    projectStore.list.find(_.outputDirectory == Some(projectName))
  }

  def getProjectFile(projectName: String, fileName: String): Try[File] = {
    for {
      project <- Try(fetchProject(projectName).get)
      outputDir <- Try(new File(new File(projectManager.projectDir, project.outputDirectory.get), "/" + fileName)) if outputDir.exists()
    } yield outputDir
  }

  def downloadFile(projectName: String, fileName: String) = Action {
    getProjectFile(projectName, fileName).map { projectZipFile =>
      Ok.sendFile(content = projectZipFile, fileName = _ => s"$projectName-$fileName")
    }.getOrElse(BadRequest(s"$fileName for project $projectName not found!"))
  }

  def projectInfo(projectName:String, from:Int) = Action {
    projectStore.list.find(_.outputDirectory == Some(projectName)).map { f =>
      val dir = new File(projectManager.projectDir, f.outputDirectory.get)
      val logO: Option[Array[String]] = for {
        jobOut <- at(dir, "out")
      } yield fileContent(jobOut, from)

      val log:Array[String] = logO.getOrElse(Array.empty[String])

      Ok(Json.obj(
        "name" → projectName,
        "generated" → "job",
        "log" → log
      ))
    }.getOrElse(BadRequest(s"Project $projectName not found!"))
  }


  def artifactoryURI: Option[((String, String), Option[(String, String)])] = {
    val pullPushOption:Option[(String, String)] = for {
      pull <- current.configuration.getString("sbt-project-generation.publishing.artifactory.pull")
      push <- current.configuration.getString("sbt-project-generation.publishing.artifactory.push")
    } yield (pull, push)
    val credOption = for {
      user <- current.configuration.getString("sbt-project-generation.publishing.artifactory.user")
      password <- current.configuration.getString("sbt-project-generation.publishing.artifactory.password")
    } yield (user, password)
    pullPushOption.map { pp =>
      pp → credOption
    }
  }

  def sbtDependencyConfig: DependencyConfig = {
    val urlResolverCloudera = current.configuration.getString("sbt-project-generation.dependencies.cloudera").getOrElse("https://repository.cloudera.com/artifactory/cloudera-repos")
    val urlResolverBintrayDataFellasMvn = current.configuration.getString("sbt-project-generation.dependencies.bintray-data-fellas-maven").getOrElse("http://dl.bintray.com/data-fellas/maven")
    val urlResolverBintraySparkPackagesMvn = current.configuration.getString("sbt-project-generation.dependencies.bintray-spark-packages").getOrElse("http://dl.bintray.com/spark-packages/maven/")
    val jdkHome = current.configuration.getString("sbt-project-generation.dependencies.jdk_home").getOrElse(throw new ConfigurationMissingException("sbt-project-generation.dependencies.jdk_home"))
    val sbtHome = current.configuration.getString("sbt-project-generation.dependencies.sbt_home").getOrElse(throw new ConfigurationMissingException("sbt-project-generation.dependencies.sbt_home"))

    val urlDockerBaseImage = current.configuration.getString("sbt-project-generation.dependencies.docker-base-image").getOrElse("data-fellas-docker-public.bintray.io/base-adst:0.0.1")
    val urlResolverSbtPlugin = current.configuration.getString("sbt-project-generation.dependencies.sbt-plugin-releases").getOrElse("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases")

    com.datafellas.g3nerator.model.DependencyConfig(
      urlResolverCloudera,
      urlResolverBintrayDataFellasMvn,
      urlResolverBintraySparkPackagesMvn,
      Paths.get(jdkHome),
      Paths.get(sbtHome),
      urlDockerBaseImage,
      urlResolverSbtPlugin,
      artifactoryURI
    )
  }


  def generateNbProject(fp: String) = Action(parse.tolerantText) { request =>
    val path = URLDecoder.decode(fp, UTF_8)
    val text = request.body
    val json = Json.parse(request.body)

    Logger.info("Generating project for notebook:" + path)
    val toDir = (json \ "to_dir").as[String]
    val pkg = (json \ "pkg").as[String]
    val version = (json \ "version").as[String]
    val maintainer = (json \ "maintainer").as[String]
    val dockerRepo = (json \ "docker_repo").as[String]
    val mesosVersion = (json \ "mesos_version").as[String]

    implicit val ec = kernelSystem.dispatcher

    // FIXME: do we need to override some configs ?!
    // val nbContent = notebookConfig.overrideConf.merge(path)//.map(notebook.Notebook.write)
    val nbContent = utils.AppUtils.notebookManager.load(path)

    nbContent match {
      case Some(nbFile) =>
        val isPublishLocal = current.configuration.getBoolean("sbt-project-generation.mode.local").getOrElse(false) // ignored

        val prod = for {
          sv <- current.configuration.getString("sbt-project-generation.code-gen.sparkVersion")
          hv <- current.configuration.getString("sbt-project-generation.code-gen.hadoopVersion")
        } yield com.datafellas.g3nerator.model.ProductionConfig(sv, hv)

        val projectConfig = ProjectConfig(
          pkg,
          version,
          maintainer,
          dockerRepo,
          "hdfs://namenode1.hdfs.mesos:50071/docker/docker.tar.gz", // TODO: not good!!!!
          mesosVersion
        )

        val project = new Project(nbFile, projectManager.projectDir, projectConfig, sbtDependencyConfig, productionConfig = prod)
        val result = projectManager.publish(project, isPublishLocal)
        result.onFailure{ case ex:Exception => Logger.error(s"Project generation failed. Reason: ${ex.getMessage}", ex)}
        Ok(Json.obj("to_dir" → toDir))
      case None =>
        BadRequest("Cannot read and merge notebook")
    }
  }
}
