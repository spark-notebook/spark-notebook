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

import com.typesafe.config._

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import notebook._
import notebook.server._
import notebook.kernel.remote._
import notebook.NBSerializer.Metadata

object TachyonProxy extends Controller {

  def ls(path:String = "/") = Action.async {

  }

}