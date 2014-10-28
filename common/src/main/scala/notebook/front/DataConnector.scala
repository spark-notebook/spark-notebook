package notebook.front

import org.json4s.JsonAST._

import notebook._, JSBus._
import notebook.front._, widgets._
import notebook.JsonCodec._

trait DataConnector[T] {
  implicit val codec:Codec[JValue, T]

  val dataConnection:ValueConnection = JSBus.createConnection
  lazy val currentData:Connection[Seq[T]] = dataConnection biMap tSeq[T]

  def apply(newData: Seq[T]) = currentData <-- Connection.just(newData)
}
