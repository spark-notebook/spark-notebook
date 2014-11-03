package notebook.front

import org.json4s.JsonAST._

import notebook._, JSBus._
import notebook.front._, widgets._
import notebook.JsonCodec._

trait SingleConnector[T] extends util.Logging {
  implicit def codec:Codec[JValue, T]

  val dataConnection:ValueConnection = JSBus.createConnection
  lazy val currentData:Connection[T] = dataConnection biMap codec

  def apply(newData: T) {
    currentData <-- Connection.just(newData)
  }
}

trait DataConnector[T] extends SingleConnector[Seq[T]] {
  implicit def singleCodec:Codec[JValue, T]
  implicit lazy val codec = tSeq[T](singleCodec)
}
