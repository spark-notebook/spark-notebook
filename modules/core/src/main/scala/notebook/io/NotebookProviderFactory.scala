package notebook.io

import com.typesafe.config.Config
import notebook.io.FutureUtil._
import notebook.util.Logging

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

object NotebookProviderFactory extends Logging {
  def createProviderConfigurator(providerFactoryClass: String): Configurable[NotebookProvider] = {
    Class.forName(providerFactoryClass).newInstance().asInstanceOf[Configurable[NotebookProvider]]
  }

  def createNotebookIoProvider(providerConfiguratorClass: String, providerConfig: Config, initTimeoutMillis: Long
                              )(implicit ec: ExecutionContext): NotebookProvider = {

    val eventualProvider: Future[NotebookProvider] = Try {
      val factory = createProviderConfigurator(providerConfiguratorClass)
      factory(providerConfig)
    }.toFuture
      .flatMap(identity)
      .recoverWith { case ex: Exception =>
        logError(s"Unable to instantiate provider from key [$providerConfiguratorClass]. Reason: [${ex.getMessage}]")
        Future.failed(ex)
      }

    Await.result(eventualProvider, new FiniteDuration(initTimeoutMillis, MILLISECONDS))
  }

}
