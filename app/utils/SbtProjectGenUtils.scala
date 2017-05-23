package utils

import notebook.io.{ConfigurationMissingException, GitProvider, GitProviderConfigurator}
import com.datafellas.g3nerator.{FileProjectStore, ProjectManager}
import com.datafellas.notebook.adastyx.server.SbtProjectGenConfig
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object SbtProjectGenUtils {

  import play.api.Play.current

  def config = {
    val conf = current.configuration.getConfig("sbt-project-generation")
      .getOrElse(throw new RuntimeException("Could not find [sbt-project-generation] configuration"))
    SbtProjectGenConfig(conf)
  }

  def projectsDirName: String = {
    config.projectsDirConf.getOrElse(throw new ConfigurationMissingException("sbt-project-generation.projects.dir"))
  }

  protected def projectsDir = config.projectsDir

  protected def providerTimeout = config.providerTimeout

  protected def gitProvider: GitProvider = {
    import collection.JavaConverters._
    val gitProjectsDirConf = ConfigFactory.parseMap(Map("local_path" -> projectsDirName).asJava)
    val gitConfig = config.underlying.withFallback(gitProjectsDirConf)
    Await.result(new GitProviderConfigurator().apply(gitConfig), providerTimeout)
  }

  lazy val projectManager = {
    Try {
      // make sure GitProvider is initialized Before creating FileProjectStore
      // (as Git provider seem to force-pull stuff, while FileProjectStore must create a new file if one does not exists)
      gitProvider
      new ProjectManager(projectsDir, projectStore.get, gitProvider)
    }
  }

  lazy val projectStore = Try(new FileProjectStore(projectsDir))

  def isConfigured: Boolean = {
    println("projectStore: ", projectStore, "projectManager:", projectManager)
    Try(config).isSuccess && projectManager.isSuccess && projectStore.isSuccess
  }
}
