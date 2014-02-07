import akka.actor.ActorSystem
import akka.pattern.AskSupport
import scala.concurrent._
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import com.bwater.notebook.client.ExecuteRequest
import com.bwater.notebook.client.{ExecuteResponse, ExecuteRequest}
import com.bwater.notebook.Kernel
import com.bwater.notebook.kernel.remote.AkkaConfigUtils
import com.bwater.notebook.server.{CalcWebSocketService, SessionRequest, WebSockWrapper}
import com.typesafe.config.ConfigFactory
import java.util.concurrent.{LinkedBlockingQueue, ArrayBlockingQueue, BlockingQueue}
import net.liftweb.json.JsonAST.JInt
import net.liftweb.json.JsonAST.{JValue, JInt}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration._
import net.liftweb.json._
import JsonDSL._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Ken
 */
class KernelTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory with AskSupport {

  def this() = this(ActorSystem("MySpec", AkkaConfigUtils.requireCookie(ConfigFactory.load("subprocess-test"), "Cookie")))

  implicit val defaultTimeout: Timeout = 20 seconds

  var startedKernels = List[Kernel]()

  override def afterAll() {
    println("Shutting down %d kernels".format(startedKernels.size))
    startedKernels map { _.shutdown() }
  }

  class CalcTester {
    val io = new TestWebSocket("io")
    val shell = new TestWebSocket("shell")
    val kernel = new Kernel(_system)
    startedKernels = kernel :: startedKernels
    val service = new CalcWebSocketService(system, List(), List(), kernel.remoteDeployFuture)
    service.ioPubPromise.success(io)
    service.shellPromise.success(shell)

    def sendCode(code:String) {
      service.calcActor ! SessionRequest(JInt(1), JInt(1), ExecuteRequest(1, code))
    }
  }

    class TestWebSocket(name: String) extends WebSockWrapper {
      val q = new LinkedBlockingQueue[JValue]()
      def send(header: JValue, session: JValue, msgType: String, content: JValue) {
        println("%s: %s".format(name, content))
        q.add(content)
      }
      def response() = Future { q.take() }
      def filteredResponse(filter: JValue => Boolean) = Future {
        var r: JValue = null
        do {
          r = q.take()
        } while (!filter(r))
        r
      }

      def awaitResponse() = Await.result(response(), 10 seconds)
      def awaitResult() = Await.result(filteredResponse(v => v \ "data" != JNothing && v \ "execution_count" != JNothing), 10 seconds) \ "data"
    }

  // Makes a singleton JObject

  "A kernel calculator" should {
    "perform simple math" in {
      val calc = new CalcTester
      calc.sendCode("1+1")
      Await.result(calc.io.response(), 10 seconds) should equal(pair2jvalue("execution_state" -> "busy"))
      Await.result(calc.io.response(), 10 seconds) should equal(("execution_count"-> 1) ~ ("code" -> "1+1"))
      Await.result(calc.io.response(), 10 seconds) should equal(("data"-> "res0: Int = 2\n") ~ ("name" -> "stdout"))
      Await.result(calc.io.response(), 10 seconds) should equal(("execution_count"-> 1) ~ ("data"-> ("text/html"-> "2")))
      Await.result(calc.io.response(), 10 seconds) should equal(pair2jvalue("execution_state" ->"idle"))
      Await.result(calc.shell.response(), 10 seconds) should equal(pair2jvalue("execution_count" -> 1))
      calc.kernel.shutdown()
    }

    "Execute calculations in order" in {
      val calc = new CalcTester
      calc.sendCode("val a1 = 1")
      for (i <- 1 to 20) {
        calc.sendCode("val a%d = 1 + a%d".format(i+1,i))
      }
      val results = (1 to 21) map { _ => (calc.io.awaitResult() \ "text/html") }
      val expected = (1 to 21) map {i => JString("%s".format(i)) }
      results should equal(expected)
    }


  }
}
