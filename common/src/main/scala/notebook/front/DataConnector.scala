package notebook.front

import org.json4s.JsonAST._

import notebook._, JSBus._
import notebook.front._, widgets._
import notebook.JsonCodec._

trait IOSingleConnector[I, O] extends util.Logging {
  implicit def codec:Codec[JValue, O]

  def toO:I=>O

  val dataConnection:ValueConnection = JSBus.createConnection
  lazy val currentData:Connection[O] = dataConnection biMap codec

  def apply(newData: I) {
    currentData <-- Connection.just(toO(newData))
  }
}

trait SingleConnector[T] extends IOSingleConnector[T,T] {
  val toO:T=>T = identity[T] _
}

trait IODataConnector[I,O] extends IOSingleConnector[Seq[I], Seq[O]] {
  implicit def singleCodec:Codec[JValue, O]
  implicit lazy val codec = tSeq[O](singleCodec)

  def singleToO:I=>O

  lazy val toO:Seq[I]=>Seq[O] = (_:Seq[I]) map singleToO
}

trait DataConnector[T] extends IODataConnector[T, T] {
  def singleToO:T=>T = identity[T]

  override lazy val toO:Seq[T]=>Seq[T] = identity[Seq[T]]
}

trait SingleConnectedWidget[T] extends SingleConnector[T] with Widget