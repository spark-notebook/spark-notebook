package notebook.front
package widgets

import scala.util.Random
import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import notebook._
import notebook.JsonCodec._

trait PipeComponent[X <: PipeComponent[X]] {
  def id:String
  def name:String
  def tpe:String
  def parameters:Map[String, String]
  def apply(a:Any):Any
  def merge(j:JsValue):X
  def toJSON:JsValue = Json.obj(
    "name" → name,
    "id" → id,
    "tpe" → tpe,
    "parameters" → parameters
  )
}

abstract class BasePipeComponent[X<:BasePipeComponent[X]] extends PipeComponent[X] {
}

abstract class BoxPipeComponent[X<:BoxPipeComponent[X]]() extends BasePipeComponent[X]() {
  val tpe = "box"
}

case class LinkPipe(id:String = java.util.UUID.randomUUID.toString,
                    source:Option[String]=None,
                    target:Option[String]=None) extends BasePipeComponent[LinkPipe]() {
  val name = "link"
  val tpe = "link"
  val parameters = source.map(x => Map("source" → x)).getOrElse(Map.empty[String, String]) ++
                   target.map(x => Map("target" → x)).getOrElse(Map.empty[String, String])
  def apply(a:Any):Any = {
    println("Applying LinkPipe with " + a)
    a
  }
  def merge(j:JsValue) = copy(source = (j \ "parameters" \ "source").asOpt[String],
                              target = (j \ "parameters" \ "target").asOpt[String])
}

case class LogPipe(id:String = java.util.UUID.randomUUID.toString) extends BoxPipeComponent[LogPipe]() {
  val name = "log"
  val parameters = Map.empty[String, String]
  def apply(a:Any):Any = {
    println("Applying LogPipe with " + a)
    a
  }
  def merge(j:JsValue):LogPipe = this
}

case class WithParamPipe( id:String = java.util.UUID.randomUUID.toString,
                          parameters:Map[String, String] = Map("testA" → "a", "testB" → "b")
                        ) extends BoxPipeComponent[WithParamPipe]() {
  val name = "withParam"
  def apply(a:Any):Any = {
    println("Applying WithParamPipe with " + a)
    a
  }
  def merge(j:JsValue):WithParamPipe = copy(
    parameters = (j \ "parameters").as[Map[String, String]]
  )
}

object Flow {
  var registeredPC:scala.collection.mutable.Map[String, ()=>BoxPipeComponent[_]] =
    scala.collection.mutable.Map(
      "log"       → (() => LogPipe()),
      "withParam" → (() => WithParamPipe())
    )

  def registerPipeComponent(name:String, creator:() => BoxPipeComponent[_]) {
    registeredPC += (name → creator)
  }

  def createPipeComponent(s:String):Option[PipeComponent[_]] = registeredPC.get(s).map(_())
}

case class Flow() extends JsWorld[PipeComponent[_], JsValue] {
  import notebook.JSBus._

  implicit val singleToO = (pc:PipeComponent[_]) => pc.toJSON

  implicit val singleCodec = idCodec[JsValue]

  override def data:Seq[PipeComponent[_]] = mutData
  private[this] var mutData:Seq[PipeComponent[_]] = Nil

  override val scripts = List(
    Script("flow", Json.obj())
  )

  private[this] var selected:Option[String] = None
  val dl = new DropDown("---" :: Flow.registeredPC.keys.toList)
  dl.selected --> Connection.fromObserver { (pc:String) =>
    pc match {
      case "---" => selected = None
      case x => selected = Some(x)
    }
  }
  val addPipeComponent = new Button(Some("+"))
  addPipeComponent.currentData --> Connection.fromObserver { (_:Double) =>
    for {
      s  <- selected
      pc <- Flow.createPipeComponent(s)
    } addAndApply(pc)
  }
  val addLink = new Button(icon=Some("arrow-right"))
  addLink.currentData --> Connection.fromObserver { (_:Double) =>
    addAndApply(new LinkPipe())
  }

  currentData --> Connection.fromObserver { (s:Seq[JsValue]) =>
    val m = s.map{ j => ((j \ "id").as[String]) → j }.toMap
    mutData = mutData.map { pc =>
      m.get(pc.id) match {
        case None => Some(pc)
        case Some(j) =>
          j match {
            case x:JsObject if x.keys.contains("remove") && (x \ "remove").as[Boolean] =>
              None
            case j =>
              Some(pc.merge(j))
          }
      }
    }.collect{ case Some(x) => x.asInstanceOf[PipeComponent[_]] }
  }

  def addAndApply(pc:PipeComponent[_]) {
    mutData = mutData :+ pc
    apply(data)
  }

  def run(init:Any) {
    // build tree
    mutData.foreach(pc => pc(init))
  }

  override def content = Some {
    <div class="container-fluid">
      <div class="control col-md-12">
        {
          dl.toHtml
        }
        {
          addPipeComponent.toHtml
        }
        {
          addLink.toHtml
        }
      </div>
      <div class="jointgraph col-md-9"></div>
      <div class="col-md-3">
        <h4>Configuration</h4>
        <form class="form configure" action="#">
          <div class="configuration">
          </div>
          <button type="button" class="btn btn-xs btn-danger remove">Remove</button>
          <button type="submit" class="btn btn-default">Apply</button>
        </form>
      </div>
    </div>
  }
}