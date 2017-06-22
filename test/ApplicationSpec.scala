// import org.junit.runner._
// import org.specs2.runner._
// import play.api.mvc._
// import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
// FIXME: it now works with `test-only ApplicationSpec` but fails with `sbt test`
//@RunWith(classOf[JUnitRunner])
//class ApplicationSpec extends PlaySpecification with Results {
//
//  "Application" should {
//      "send 404 on a bad request" in new WithApplication {
//        val result = route(FakeRequest(GET, "/boum")).get
//        status(result) must equalTo(NOT_FOUND)
//      }
//
//      "render the index page" in new WithApplication {
//        val home = route(FakeRequest(GET, "/")).get
//
//        status(home) must equalTo(OK)
//        contentType(home) must beSome.which(_ == "text/html")
//
//        contentAsString(home) must contain("Spark Notebook</a>")
//      }
//  }
//}
