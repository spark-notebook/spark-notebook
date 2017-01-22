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
  // inputs (one Any per inPort String) → outputs (one Any per outPort String)
  def next(a:Map[String, Any]):Map[String, Any]
  def merge(j:JsValue):X
  private[front] val me:X = this.asInstanceOf[X]
  def toJSON:JsObject = Json.obj(
    "name" → name,
    "id" → id,
    "tpe" → tpe,
    "parameters" → parameters
  )
}

abstract class BasePipeComponent[X<:BasePipeComponent[X]] extends PipeComponent[X] {
}

abstract class LinkPipeComponent[X<:LinkPipeComponent[X]]() extends BasePipeComponent[X]() {
  val tpe = "link"
  def source:Option[(String, String)]
  def target:Option[(String, String)]
}

abstract class BoxPipeComponent[X<:BoxPipeComponent[X]]() extends BasePipeComponent[X]() {
  val tpe = "box"
  def inPorts:List[String]
  def outPorts:List[String]
  def position:(Int, Int)
  def size:(Int, Int)
  def update(varName:String, i:notebook.util.InterpreterUtil, s:String):Unit = ()
  override def toJSON:JsObject = super.toJSON ++ Json.obj(
                                              "inPorts" → inPorts,
                                              "outPorts" → outPorts,
                                              "position" → Json.obj(
                                                  "x" → position._1,
                                                  "y" → position._2
                                                ),
                                              "size" → Json.obj(
                                                  "width" → size._1,
                                                  "height" → size._2
                                                )
                                            )
}

case class LinkPipe(id:String = java.util.UUID.randomUUID.toString,
                    source:Option[(String, String)]=None,
                    target:Option[(String, String)]=None) extends LinkPipeComponent[LinkPipe]() {
  val name = "link"
  def goe(o:Option[String], e:String) = o.map(x => Map(e → x)).getOrElse(Map.empty[String, String])
  val parameters = goe(source.map(_._1), "source_id") ++ goe(source.map(_._2), "source_port") ++
                    goe(target.map(_._1), "target_id") ++ goe(target.map(_._2), "target_port")
  def next(a:Map[String, Any]):Map[String, Any] = {
    println("Applying next on LinkPipe with " + a)
    a
  }
  def getSource(j:JsValue, k:String) = (j \ "parameters" \ s"source_$k" ).asOpt[String]
  def getTarget(j:JsValue, k:String) = (j \ "parameters" \ s"target_$k" ).asOpt[String]
  def merge(j:JsValue) = {
    copy(
      id     = (j \ "id").as[String],
      source = for (i <- getSource(j, "id"); p <- getSource(j, "port")) yield (i,p),
      target = for (i <- getTarget(j, "id"); p <-getTarget(j, "port")) yield (i,p)
    )
  }
}

case class LogPipe(
  id:String = java.util.UUID.randomUUID.toString,
  inPorts:List[String] = List("in"),
  outPorts:List[String] = List("out"),
  position:(Int, Int) = (100, 100),
  size:(Int, Int) = (100, 60)
) extends BoxPipeComponent[LogPipe]() {
  val name = "log"
  val parameters = Map.empty[String, String]

  def next(a:Map[String, Any]):Map[String, Any] = {
    println("Applying next on LogPipe with " + a)
    a
  }
  def merge(j:JsValue):LogPipe = this
}

trait Updatable {
  def update( varName:String,
              i:notebook.util.InterpreterUtil
            ):Unit
}

case class CustomizableBoxPipe(
  id:String = java.util.UUID.randomUUID.toString,
  inPorts:List[String] = List("in"),
  outPorts:List[String] = List("out"),
  position:(Int, Int) = (100, 100),
  size:(Int, Int) = (100, 60),
  parameters:Map[String, String] = Map("next" → "(a:Map[String, Any])=>a")
) extends BoxPipeComponent[CustomizableBoxPipe]() with Updatable {
  val name = "customizable"

  var _next:Map[String, Any]=>Map[String, Any] = _
  def next(a:Map[String, Any]):Map[String, Any] = _next(a)

  def merge(j:JsValue):CustomizableBoxPipe = copy(
    id         = (j \ "id").as[String],
    inPorts    = (j \ "inPorts").as[List[String]],
    outPorts   = (j \ "outPorts").as[List[String]],
    position   = ((j \ "position" \ "x").as[Int], (j \ "position" \ "y").as[Int]),
    size       = ((j \ "size" \ "width").as[Int], (j \ "size" \ "height").as[Int]),
    parameters = (j \ "parameters").as[Map[String, String]]
  )

  override def update(
              varName:String,
              i:notebook.util.InterpreterUtil
            ):Unit = {
    i{
      varName + s""".data.find(_.id == "$id").map{p => p.asInstanceOf[CustomizableBoxPipe]}.""" +
        "foreach{p => p._next = { " + parameters("next") + "}.asInstanceOf[Map[String, Any]=>Map[String, Any]] }"
    }
  }
}

object Flow {
  var registeredPC:scala.collection.mutable.Map[String, ()=>BoxPipeComponent[_]] =
    scala.collection.mutable.Map(
      "log"       → (() => LogPipe()),
      "customizable" → (() => CustomizableBoxPipe())
    )

  def registerPipeComponent(name:String, creator:() => BoxPipeComponent[_]) {
    registeredPC += (name → creator)
  }

  def createPipeComponent(s:String):Option[PipeComponent[_]] = registeredPC.get(s).map(_())
}

case class Flow() extends Updatable with JsWorld[PipeComponent[_], JsValue] {
  import notebook.JSBus._

  implicit val singleToO = (pc:PipeComponent[_]) => pc.toJSON

  implicit val singleCodec = idCodec[JsValue]

  override def data:Seq[PipeComponent[_]] = mutData
  private[this] var mutData:Seq[PipeComponent[_]] = Nil

  val confConnection = notebook.JSBus.createConnection
  confConnection --> Connection.fromObserver { (js:JsValue) =>
    load(js)
  }

  val linkConnection = notebook.JSBus.createConnection
  linkConnection --> Connection.fromObserver { (js:JsValue) =>
    val l = new LinkPipe()
    val u = l.merge(js)
    addAndApply(u)
  }

  override val scripts = List(
    Script("flow", Json.obj("confId" → confConnection.id, "linkId" → linkConnection.id))
  )

  def load(js:JsValue) = {
    val JsArray(array) = js
    val pcs = array.map { j =>
      val JsString(name) = j \ "name"
      val pc = if (name == "link") {//hackish :-S
        Some(new LinkPipe())
      } else {
        Flow.createPipeComponent(name)
      }
      val p = pc.map { p =>
        val pc:PipeComponent[_] = p.merge(j).asInstanceOf[PipeComponent[_]]
        pc
      }
      p
    }.flatten
    addAndApply(pcs)
  }

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

  currentData --> Connection.fromObserver { (s:Seq[JsValue]) =>
    val m = s.map{ j => ((j \ "id").as[String]) → j }.toMap
    mutData = mutData.map { pc =>
      m.get(pc.id) match {
        case None => Some(pc.me)
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
    addAndApply(Seq[PipeComponent[_]](pc))
  }

  def addAndApply(pcs:Seq[PipeComponent[_]]) {
    mutData = mutData ++ pcs
    apply(data)
  }

  override def update(
              varName:String,
              i:notebook.util.InterpreterUtil
            ):Unit =  data.filter(_.isInstanceOf[Updatable])
                          .foreach(_.asInstanceOf[Updatable].update(varName, i))

  /**
   * @param init is a function that take a source box and gives it a init value
   */
  def run(init:(String /*BoxId*/, List[String/*InPort*/]) => Map[String/*OutPort*/, Any]):scala.collection.Map[(String, String),Any] = {
    // build tree
    val currentData:List[PipeComponent[_]] = mutData.toList

    val (links, boxes) = {
      val (ls, bxs) = currentData.partition(_.isInstanceOf[LinkPipeComponent[_]])
      val links = ls.map(_.asInstanceOf[LinkPipeComponent[_]])
      val boxes = bxs.map(_.asInstanceOf[BoxPipeComponent[_]])
      (links.filter(l => l.source.isDefined && l.target.isDefined), boxes)
    }
    /**
     *  X ----> Z ------> U ----> A
     *  Y --/         /
     *  W -----------/
     *\____/  \_____/  \_____/  \____/
     * L0       L1        L2      L3
     */

    def layer(remaning:Seq[BoxPipeComponent[_]], acc:List[Seq[BoxPipeComponent[_]]]):List[Seq[BoxPipeComponent[_]]] = {
      remaning match {
        case Nil          => acc.reverse

        case `boxes`      =>
          val layer0 =  remaning.filterNot { b =>
                          links.exists(l => l.target == Some(b.id))
                        }
          layer(remaning diff layer0, List(layer0))

        case xs  =>
          val next = xs.filter { b =>
            val targetB = links.filter(l => l.target == Some(b.id))

            targetB.forall { l =>
              acc.flatten.exists(p => l.source == Some(p.id))
            }
          }
          layer(remaning diff next, next :: acc)
      }
    }
    val layers = layer(boxes, Nil)

    val values = scala.collection.mutable.Map.empty[(String /*BoxID*/, String /*OutPort*/), Any]

    val results = layers.map { pcs =>
      pcs.foreach { pc =>
        val linksToPc = links.map { link =>
                                val pcid = pc.id
                                link.target match {
                                  case Some((`pcid`, port)) if pc.inPorts.contains(port) => Some(port → link.source.get)
                                  case _ => None
                                }
                              }.collect{case Some(x) => x}

        val valuesForLinksSource = linksToPc.map{ case (inPort, (srcId, outPort)) =>
                                              for{
                                                src <- currentData.find(_.id == srcId)
                                                v   <- values.get((srcId, outPort))
                                              } yield (inPort → v)
                                            }
                                            .collect{ case Some(x) => x }
                                            .toMap

        values ++= (valuesForLinksSource match {
          case xs if xs.isEmpty => pc.next(init(pc.id, pc.inPorts))
          case xs               => pc.next(xs.asInstanceOf[Map[String, Any]])
        }).map{ case (outPort, v) => (pc.id, outPort) → v}
      }
    }
    val lastPCs = boxes.filterNot { b =>
                    links.exists(l => l.source.map(_._1) == Some(b.id))
                  }

    val rs = values.filterKeys(k => lastPCs.exists(_.id == k._1))
    rs
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
