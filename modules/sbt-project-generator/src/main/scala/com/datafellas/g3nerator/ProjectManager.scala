package com.datafellas.g3nerator

import java.io.File
import java.net.URL

import scala.io.Source
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.datafellas.g3nerator.model._
import notebook.io.GitProvider
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectManager(val projectDir: File, projectStore: ProjectConfigStore, gitProvider: GitProvider) {

  private val gitignore: File = {
    val f = new File(projectDir, ".gitignore")
    if (!f.exists) f.createNewFile

    val contents = List(
      "**/target",
      "*/jdk",
      "*/jdk-*",
      "*/sbt",
      "out"
    )

    val igs = scala.io.Source.fromFile(f).getLines.toList
    val toAdd = contents.filter { l => !igs.exists(_ == l) }
    val w = new java.io.FileWriter(f, true)
    toAdd.foreach(s => w.append(s+"\n"))
    w.close
    f
  }

  def publish(project: Project, isPublishLocal:Boolean): Future[Unit] = {
    gitProvider.refreshLocal()
    val job = project.mkJob

    for {
      // we first add to the store, we should update it when terminated
      // this is important to track the progress (otherwise it won't be available there until the whole gen is done)
      _ <- Future(projectStore.add(project.config))
      _ <- job.publish(isPublishLocal)
      // P.S. if remote git is not configured this only publishes to a local git instance
      _ <- gitProvider.add(project.config.outputDirectory.get, "Added generated project " + project.name)
      _ <- gitProvider.push()
    } yield ()
  }
}

trait ProjectConfigStore {
  def replaceAll(projectConfigs: List[ProjectConfig]) : Unit
  def add(projectConfig: ProjectConfig): Unit
  def remove(project: Project): Unit = ???
  def list: List[ProjectConfig]
}

class FileProjectStore(projectDir: File) extends ProjectConfigStore {

  val logger = LoggerFactory.getLogger(classOf[FileProjectStore])

  private val store: File = try {
    new File(projectDir, FileProjectStore.StoreFile)
  } catch {
    case t: Throwable => logger.error("Unable to create Project Store File", t)
      throw new RuntimeException("Unable to create Project Store File", t)
  }

  try {
    if (!store.exists) {
      store.getParentFile.mkdirs()
      store.createNewFile
      replaceAll(Nil)
    }
  } catch {
    case t: Throwable => logger.error("Unable to initialize project store", t)
      throw new RuntimeException("Unable to initialize project store", t)
  }

  def list: List[ProjectConfig] = {
    val c = Source.fromFile(store).getLines.mkString("\n")
    val json = Json.parse(c)
    ProjectConfigJsonSupport.fromJson(json)
  }

  def replaceAll(projects: List[ProjectConfig]) : Unit = {
    val json = ProjectConfigJsonSupport.toJson(projects)
    FileUtils.writeTo(store)(Json.stringify(json))
  }

  def add(p: ProjectConfig) = {
    val allProjects = p :: list.filter(other => other.name != p.name )
    replaceAll(allProjects)
  }

}
object FileProjectStore {
  val StoreFile = "generated_projects.json"
}

object ProjectConfigJsonSupport {

  implicit val readUrl:Reads[URL] = new Reads[URL] {
    override def reads(json: JsValue): JsResult[URL] = {
      JsSuccess(new URL(json.as[String]))
    }
  }
  implicit val writeUrl:Writes[URL] = new Writes[URL] {
    def writes(url:URL) = Json.toJson(url.toString)
  }

  implicit val readCredentials:Reads[Credentials] = (
    (JsPath \ Credentials.Username).read[String] and
      (JsPath \ Credentials.Password).read[String])(Credentials.apply _)

  implicit val writeCredentials: Writes[Credentials] = (
    (JsPath \ Credentials.Username).write[String] and
      (JsPath \ Credentials.Password).write[String])(unlift(Credentials.unapply))

  implicit val readSecuredUrl:Reads[SecuredUrl] = (
    (JsPath \ SecuredUrl.Url).read[URL] and
      (JsPath \ SecuredUrl.Authentication).read[Option[Credentials]]
    )(SecuredUrl.apply _)

  implicit val writeSecuredUrl:Writes[SecuredUrl] = (
    (JsPath \ SecuredUrl.Url).write[URL] and
      (JsPath \ SecuredUrl.Authentication).write[Option[Credentials]]
    )(unlift(SecuredUrl.unapply))


  implicit val projectRead: Reads[ProjectConfig] = (
    (JsPath \ "pkg").read[String] and
      (JsPath \ "version").read[String] and
      (JsPath \ "maintainer").read[String] and
      (JsPath \ "dockerRepo").read[String] and
      (JsPath \ "dockercfg").read[String] and
      (JsPath \ "mesosVersion").read[String] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "outputDirectory").readNullable[String]
    )(ProjectConfig.apply _)

  implicit val projectWrite: Writes[ProjectConfig] = (
    (JsPath \ "pkg").write[String] and
      (JsPath \ "version").write[String] and
      (JsPath \ "maintainer").write[String] and
      (JsPath \ "dockerRepo").write[String] and
      (JsPath \ "dockercfg").write[String] and
      (JsPath \ "mesosVersion").write[String] and
      (JsPath \ "name").writeNullable[String] and
      (JsPath \ "outputDirectory").writeNullable[String]
    )(unlift(ProjectConfig.unapply))

  def toJson(config: ProjectConfig): JsValue = Json.toJson(config)
  def toJson(projects: List[ProjectConfig]): JsValue = Json.toJson(projects)
  def fromJson(js:JsValue): List[ProjectConfig] = Json.fromJson[List[ProjectConfig]](js) match {
    case jsConfig: JsSuccess[List[ProjectConfig]] =>  jsConfig.get
    case err: JsError => throw new RuntimeException("Unable to parse Project Configuration:" + err.toString)
  }
}
