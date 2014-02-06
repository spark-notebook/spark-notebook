import com.bwater.notebook.NBSerializer._
import com.bwater.notebook.server.{ClientAuth, Dispatcher}
import com.bwater.notebook.{NBSerializer, Server}
import java.io.{InputStreamReader, BufferedInputStream, File}
import java.util.concurrent.CountDownLatch
import org.apache.http.client.methods.{HttpPut, HttpPost, HttpGet}
import org.apache.http.client.params.ClientPNames
import org.apache.http.entity.{StringEntity, FileEntity}
import org.apache.http.impl.client.{BasicCookieStore, DefaultHttpClient}
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.protocol.HttpContext
import org.apache.http.{Header, HttpRequest, HttpRequestInterceptor, HttpResponse}
import org.apache.log4j.PropertyConfigurator
import org.scalatest.Matchers
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import scala.concurrent.ops._
import unfiltered.request.{PUT, POST, GET}
import org.apache.http.client.methods.HttpDelete
import unfiltered.request.DELETE
import java.util.Date
import java.util.Calendar
import com.bwater.notebook.server.DispatcherSecurity
import com.bwater.notebook.server.ScalaNotebookConfig

class ClientAuthTests extends WordSpec with Matchers with BeforeAndAfterAll {

  val ServerPort = 8898
  val ServerHost = "127.0.0.1"

  @volatile var server: unfiltered.netty.Http = _
  @volatile var serverThread: Thread = _
  @volatile var dispatcher: Dispatcher = _
  @volatile var security: ClientAuth = new ClientAuth(ServerHost, ServerPort)

  override def beforeAll() {
    val serverStarted = new CountDownLatch(1)
    spawn {
      serverThread = Thread.currentThread()
      Thread.currentThread().setName("main") // Unfiltered exits upon input via System.in unless thread name is 'main' !?
      //Thread.currentThread().setDaemon(true)
      Server.startServer(ScalaNotebookConfig.defaults, ServerHost, ServerPort, security) {
        (http, app) =>
          server = http
          dispatcher = app
          serverStarted.countDown()
      }
    }
    serverStarted.await()
  }

  override def afterAll() {
    server.stop()
    serverThread.interrupt()
  }

  val testnb = Notebook(new Metadata("sample"), List(Worksheet(List(CodeCell("1+2", "python", false, Some(2), List(ScalaOutput(2, None, Some("3"))))))), Nil, None)

  def sessionCookie = {
    val sessCookie = new BasicClientCookie("sessID" + ServerPort, security.sessionKey)
    sessCookie.setDomain(ServerHost)
    sessCookie.setPath("/")
    sessCookie
  }

  def serverURL(path: String = "/") = "http://%s:%d%s".format(ServerHost, ServerPort, path)

  def httpClient(sessionKey: Boolean = false, csrfKey: Boolean = false) = {
    val client = new DefaultHttpClient()
    client.getParams.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false)
    if (sessionKey) client.getCookieStore.addCookie(sessionCookie)
    if (csrfKey) client.addRequestInterceptor(new HttpRequestInterceptor {
      def process(request: HttpRequest, context: HttpContext) {
        request.addHeader("CSRF-Key", security.csrfKey)
      }
    })
    client
  }

  def httpGet(path: String, sessCookie: Boolean = false, csrfHeader:Boolean = false): HttpResponse = {
    val request = new HttpGet(serverURL(path))
    httpClient(sessCookie, csrfHeader).execute(request)
  }

  def httpPost(path: String, postData: String, sessCookie: Boolean = false, csrfHeader:Boolean = false): HttpResponse = {
    val request = new HttpPost(serverURL(path))
    request.setEntity(new StringEntity(postData))
    httpClient(sessCookie, csrfHeader).execute(request)
  }

  def httpPut(path: String, postData: String, sessCookie: Boolean = false, csrfHeader:Boolean = false): HttpResponse = {
    val request = new HttpPut(serverURL(path))
    request.setEntity(new StringEntity(postData))
    httpClient(sessCookie, csrfHeader).execute(request)
  }

  def httpDelete(path: String, sessCookie: Boolean = false, csrfHeader:Boolean = false): HttpResponse = {
    val request = new HttpDelete(serverURL(path))
    httpClient(sessCookie, csrfHeader).execute(request)
  }

  def httpWSUpgrade(path: String, sessCookie: Boolean = false, csrfHeader:Boolean = false): HttpResponse = {
    val request = new HttpGet(serverURL(path))
    request.addHeader("Upgrade", "websocket")
    request.addHeader("Connection", "Upgrade")
    request.addHeader("Sec-Websocket-Key", "dGhlIHNhbXBsZSBub25jZQ==")
    request.addHeader("Origin", "http://%s".format(ServerHost))
    httpClient(sessCookie, csrfHeader).execute(request)
  }

  def assertResponseCode(code: Int, path: String, sessionCookie: Boolean = false, csrfHeader:Boolean = false, method: unfiltered.request.Method = GET, data: String = "") = {
    val response = method match {
      case GET => httpGet(path, sessionCookie, csrfHeader)
      case POST => httpPost(path, data, sessionCookie, csrfHeader)
      case PUT => httpPut(path, data, sessionCookie, csrfHeader)
      case DELETE => httpDelete(path, sessionCookie, csrfHeader)
    }
    assert (code === response.getStatusLine.getStatusCode, "Expected %d code for resource '%s' but response was (%d: %s)".format(code, path, response.getStatusLine.getStatusCode, response.getStatusLine.getReasonPhrase))
    response
  }

  def parseCookies(resp: HttpResponse) = {
    val mBuild = Map.newBuilder[String, String]
    val it = new BasicHeaderElementIterator(resp.headerIterator("Set-Cookie"))
    while (it.hasNext) {
      val el = it.nextElement()
      mBuild += el.getName -> el.getValue
    }
    mBuild.result()
  }

  "An unauthenticated client" should {
    "not access any resource" in {
      assertResponseCode(403, "/") // Template
      assertResponseCode(403, "/static/js/csrf.js") // Static resource
      assertResponseCode(403, "/static/js/notebooklist.js") // Static resource
      assertResponseCode(403, "/notebooks") // Dynamic query (notebook list)
      assertResponseCode(403, "/notebooks/sample", method=DELETE) // Delete
      assertResponseCode(403, "/kernels", method=POST) // Create kernel
      assertResponseCode(403, "/kernels/kernel_id/restart", method=POST) // restart kernel (bad ID, but 403 indicates unauthorized)
      assertResponseCode(403, "/kernels/kernel_id/interrupt", method=POST) // interrupt kernel
    }

    "not initialize websockets" in {
      assert( 403 === httpWSUpgrade("/kernels/fake_kID/iopub").getStatusLine.getStatusCode)
      assert( 403 === httpWSUpgrade("/kernels/fake_kID/shell").getStatusLine.getStatusCode)
    }
  }

  "An authenticated client" should {
    "have access to resources" in {
      assertResponseCode(200, "/", true)
      assertResponseCode(200, "/static/js/csrf.js", true)
      assertResponseCode(200, "/notebooks", true) //Notebook listing OK
    }

    "not have access to kernel functions without CSRF key" in {
      assertResponseCode(403, "/kernels", true, method=POST) // Create kernel
      assertResponseCode(403, "/kernels/kernel_id/restart", true, method=POST) // restart kernel (bad ID, but 403 indicates unauthorized)
      assertResponseCode(403, "/kernels/kernel_id/interrupt", true, method=POST) // interrupt kernel
    }

    "have access to kernel functions with CSRF key" in {
      assertResponseCode(200, "/kernels", true, true, method=POST) // Create kernel
      assertResponseCode(200, "/kernels/kernel_id/restart", true, true, method=POST) // restart kernel (bad ID, but 403 indicates unauthorized)
      assertResponseCode(200, "/kernels/kernel_id/interrupt", true, true, method=POST) // interrupt kernel
    }

    "not have access to notebook functions without CSRF key" in {
      assertResponseCode(403, "/notebooks/nbName", true, method=PUT, data= NBSerializer.write(testnb)) // Save/Create notebook
      assertResponseCode(403, "/notebooks/sample", true, method=DELETE) //Delete
      assertResponseCode(403, "/copy/sample", true) //copy
      assertResponseCode(403, "/copy/sample", true, method=POST) //copy
      assertResponseCode(403, "/new", true) //New notebook
      assertResponseCode(403, "/new", true, method=POST) //New notebook
    }

    "have access to notebook functions with CSRF key" in {
      assertResponseCode(200, "/notebooks/sample", true, true, method=PUT, data= NBSerializer.write(testnb)) // Save/Create notebook
      assertResponseCode(200, "/notebooks/sample", true, true) // Read notebook
      assertResponseCode(302, "/copy/sample", true, true) //copy
      assertResponseCode(200, "/notebooks/sample1", true, true) // Read notebook
      assertResponseCode(200, "/notebooks/sample", true, true, method=DELETE) //Delete
      assertResponseCode(200, "/notebooks/sample1", true, true, method=DELETE) //Delete
      new File("sample.snb").delete()
      new File("sample1.snb").delete()
      assertResponseCode(404, "/notebooks/noNB", true, true) // Read notebook (not found)
      assertResponseCode(302, "/new", true, true) //New notebook
      new File("Untitled1.snb").delete()
    }

    // HttpClient does not appear to be able to participate in the websocket handshake, this results in a hang.
    //"not have access to websocket init without CSRF key" in {
    //  assert( 403 === httpWSUpgrade("/kernels/fake_kID/iopub", true).getStatusLine.getStatusCode)
    //  assert( 403 === httpWSUpgrade("/kernels/fake_kID/shell", true).getStatusLine.getStatusCode)
    //}
  }

  "Login credentials" should {
    "yield 403 for incorrect credentials" in {
      assertResponseCode(403, "/login/%s".format("Bad-token"))
    }

    "yield session and CSRF keys" in {
      val response = assertResponseCode(302, "/login/%s".format(security.loginToken)) //Expect redirect to '/'
      val cookies = parseCookies(response)
      assert(cookies.get("sessID" + ServerPort) === Some(security.sessionKey))
      assert(cookies.get("CSRF-Key") === Some(security.csrfKey))
    }

    "expire after a single use" in {
      assertResponseCode(403, "/login/%s".format(security.loginToken))
    }
  }
}
