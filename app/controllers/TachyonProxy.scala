package controllers

import java.util.UUID
import java.io.File
import java.net.URLDecoder

import scala.collection.JavaConverters._
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Try, Success, Failure}

import play.api._
import play.api.mvc._
import play.api.mvc.BodyParsers.parse._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee._
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json._

import com.typesafe.config._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import tachyon.client.TachyonFS

import notebook._
import notebook.server._
import notebook.kernel.remote._
import notebook.NBSerializer.Metadata

object TachyonProxy extends Controller {
  lazy val tachyonUrl = AppUtils.config.tachyonInfo.url
  lazy val shareDir = AppUtils.config.tachyonInfo.baseDir
  lazy val tachyonClient = tachyonUrl.map( url => TachyonFS.get(url)).getOrElse(notebook.share.Tachyon.fs)

  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  def ls(path:String = shareDir) = Action.async { Future {
    val list = tachyonClient.ls(path, false).asScala
    Ok(Json.toJson(list))
  } }


  def tachyonJavascriptRoutes = Action { implicit request =>
    Ok(
      Routes.javascriptRouter("tachyonJsRoutes")(
        routes.javascript.TachyonProxy.ls
      )
    ).as("text/javascript")
  }
}