package notebook.front
package widgets

import scala.util.Random
import scala.xml.{NodeSeq, UnprefixedAttribute, Null}
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import notebook._
import notebook.JsonCodec._

trait PipeComponent {
  def id:String
  def name:String
  def parents:List[PipeComponent]
  def parameters:Map[String, String]
  def apply(a:Any):Any
  def toJSON:JsValue = Json.obj(
    "name" → name,
    "id" → id,
    "parents" → parents.map(_.id),
    "parameters" → parameters
  )
}

abstract class BasePipeComponent(val id:String = java.util.UUID.randomUUID.toString, val parents:List[PipeComponent] = Nil)
  extends PipeComponent {

}

case class LogPipe(override val parents:List[PipeComponent]=Nil) extends BasePipeComponent(parents=parents) {
  val name = "log"
  val parameters = Map.empty[String, String]
  def apply(a:Any):Any = {
    println("Applying LogPipe with " + a)
    a
  }
}

case class TestPipe(override val parents:List[PipeComponent]=Nil) extends BasePipeComponent(parents=parents) {
  val name = "test"
  val parameters = Map.empty[String, String]
  def apply(a:Any):Any = {
    println("Applying TestPipe with " + a)
    a
  }
}

case class WithParamPipe(override val parents:List[PipeComponent]=Nil) extends BasePipeComponent(parents=parents) {
  val name = "withParam"
  val parameters = Map("testA" → "a", "testB" → "b")
  def apply(a:Any):Any = {
    println("Applying WithParamPipe with " + a)
    a
  }
}

object Flow {
  var registeredPC = scala.collection.mutable.Map(
      "test"      → (() => TestPipe()),
      "log"       → (() => LogPipe()),
      "withParam" → (() => WithParamPipe())
    )

  def createPipeComponent(s:String):Option[PipeComponent] = registeredPC.get(s).map(_())
}

case class Flow() extends JsWorld[PipeComponent, JsValue] {
  import notebook.JSBus._

  implicit val singleToO = (pc:PipeComponent) => pc.toJSON

  implicit val singleCodec = idCodec[JsValue]

  override def data:Seq[PipeComponent] = mutData
  private[this] var mutData:Seq[PipeComponent] = Nil

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
  val add = new Button("+")
  add.currentData --> Connection.fromObserver { (_:Double) =>
    for {
      s  <- selected
      pc <- Flow.createPipeComponent(s)
    } addAndApply(pc)
  }

  def addAndApply(pc:PipeComponent) {
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
          add.toHtml
        }
        <button type="button" class="btn btn-xs">
          <span><i class="fa fa-arrow-right"/></span>
        </button>
      </div>
      <div class="jointgraph col-md-9"></div>
      <div class="col-md-3">
        <h4>Configuration</h4>
        <form class="form">
          <div class="configuration">
          </div>
        </form>
      </div>
    </div>
  }
}