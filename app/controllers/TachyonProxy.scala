package controllers

import java.net.URLDecoder

import play.api._
import play.api.libs.json._
import play.api.mvc._
import utils.Const.UTF_8
import utils.{Const, AppUtils}

import scala.concurrent.Future

object TachyonProxy extends Controller {
  lazy val tachyonUrl = AppUtils.config.tachyonInfo.flatMap(_.url)
  lazy val shareDir = AppUtils.config.tachyonInfo.map(_.baseDir).getOrElse("/share")
  lazy val tachyonClient = AppUtils.config.tachyonInfo.map(_ => new notebook.share.Client(tachyonUrl))

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def ls(p: String = shareDir) = Action.async {
    tachyonClient match {
      case None =>
        Future.successful(BadRequest("Tachyon disabled"))
      case Some(client) =>
        Future {
          val path = URLDecoder.decode(p, UTF_8)
          val list = client.list(path)
          Ok(Json.toJson(list))
        }
    }
  }


  def tachyonJavascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("tachyonJsRoutes")(routes.javascript.TachyonProxy.ls)
    ).as("text/javascript")
  }
}