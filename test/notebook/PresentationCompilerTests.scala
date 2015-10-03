package notebook

import notebook.util.Match
import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._

@RunWith(classOf[JUnitRunner])
class PresentationCompilerTests extends Specification {

  "complete" should {
    "return the correct completions" in {
      val pc = new PresentationCompiler()
      pc.complete("val test = 123\ntest.toS", 23) must beEqualTo("toS", Seq(
        Match("toString", Map.empty[String,String]),
        Match("toShort", Map.empty[String,String])
      ))
      pc.complete("val test = 123\ntest.toS\nval testAsString = test.toString()", 23) must beEqualTo("toS", Seq(
        Match("toString", Map.empty[String,String]),
        Match("toShort", Map.empty[String,String])
      ))
    }

    "have the correct amount of completions" in {
      val pc = new PresentationCompiler()
      pc.complete("val test = 123\nval testAsString = test.toString()\ntestAsString.", 63)._2.length must beEqualTo(213)
      pc.complete("val test = \"Hello World!\"\ntest.substring(", 41)._2.length must beEqualTo(631)
    }
  }
}
