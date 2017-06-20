package notebook

import notebook.util.Match
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class PresentationCompilerTests extends WordSpec with Matchers with BeforeAndAfterAll {

  def complete(pc:PresentationCompiler)(s:String, i:Int) = {
    val (st, com) = pc.complete(s, i)
    (st, com.toSet)
  }

  "complete" should {
    val cz = """|
      |class AnExample(val aField:Int, notAField:Boolean=true) {
      |  def testVar:String = ""
      |  def testMethod(a:String):String = ""
      |  def testMethod(a:String, b:String):String = ""
      |  def testMethod(a:Int, optionalB: String = ""):String = ""
      |  lazy val toSchwarz:Float = 1f
      |}
      |implicit class AnExampleWithImplicits(cls: AnExample) {
      |  def implicitMethod(a: Int): Int = 1
      |}
      |""".stripMargin

    val newInst = "val test = new AnExample(123)"
    val newLine = "\n"

    "return the correct completions" in {
      if ( sys.env.contains("SKIP_WHEN_TRAVIS") ) {
        cancel(": Test skipped on CI, causes StackOverflowError (REPL compiler bug).")
      }

      val line = "test.toS"
      val code = List(newInst, newLine, line).mkString

      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)

      val c = complete(pc) _
      c(code, code.size) shouldBe ("toS", Set(
        Match("toSchwarz", Map("display_text" -> "toSchwarz: Float")),
        Match("toString", Map("display_text" -> "toString: String"))
      ))
      val r = c(code + "\nval testAsSt$ring = test.toString()", code.size) shouldBe ("toS", Set(
        Match("toSchwarz", Map("display_text" -> "toSchwarz: Float")),
        Match("toString", Map("display_text" -> "toString: String"))
      ))
      pc.stop()
      r
    }

    "lists all overrided method versions, indicating optional parameters if any" in {
      val line = "test.testMeth"
      val code = List(newInst, newLine, line).mkString

      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)
      val c = complete(pc) _

      val r = c(code, code.size) shouldBe ("testMeth", Set(
        Match("testMethod(a: Int, [optionalB: String])",
          Map("display_text" -> "testMethod(a: Int, [optionalB: String]): String")),
        Match("testMethod(a: String)", Map("display_text" -> "testMethod(a: String): String")),
        Match("testMethod(a: String, b: String)", Map("display_text" -> "testMethod(a: String, b: String): String"))
      ))
      pc.stop()
      r
    }

    "lists the methods inherited and the implicit methods" in {
      if ( sys.env.contains("SKIP_WHEN_TRAVIS") ) {
        // Compiler exception during call to 'ask'  (PresentationCompiler.scala:59)
        // 	at scala.tools.nsc.interactive.Global.pollForWork(Global.scala:324)
        cancel(": Test skipped on CI, causes StackOverflowError (REPL compiler bug).")
      }


      val pc = new PresentationCompiler(Nil)
      pc.addScripts(cz)

      val c = complete(pc) _

      val code1 = List(newInst, newLine, "test.").mkString
      val suggestions: Set[String] = c(code1, code1.size)._2.map {case Match(s, _) => s }
      println(suggestions.map(s=> s""""${s}""""))

      val r = suggestions should contain allElementsOf Seq(
        "+(other: String)",
        "clone",
        "hashCode",
        "asInstanceOf",
        "getClass",
        "isInstanceOf",
        "implicitMethod(a: Int)"
      )

      pc.stop()
      r
    }
  }
}
