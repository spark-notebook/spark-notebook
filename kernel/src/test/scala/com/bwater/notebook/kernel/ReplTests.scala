package com.bwater.notebook.kernel

import org.scalatest.{BeforeAndAfter, FunSuite}
import com.bwater.notebook.Match
import xml.Text
import org.scalatest.Matchers
import concurrent.ops
import java.util.concurrent.{TimeUnit, CountDownLatch}

class ReplTests extends FunSuite with BeforeAndAfter with Matchers {

  var repl: Repl = _

  before {
    repl = new Repl
  }

  test("evaluating should return toString when no other renderer") {
    val actual = evaluateForSuccess("\"foobar\"")
    assert(actual === "foobar")
  }

  test("evaluating should return result when no error") {
    val actual = evaluateForSuccess("1")
    assert(actual === "1")
  }

  test("evaluating should render to html") {
    repl.evaluate(
      """
        |implicit val testRenderer = new com.bwater.notebook.Renderer[String] {
        |  def render(value: String) = {
        |    <abc>{value}</abc>
        |  }
        |}
      """.stripMargin)

    val (Success(actual), _) = repl.evaluate("\"foobar\"")
    assert(actual === <abc>foobar</abc>)
  }

  test("evaluation should return the object for val definition") {
    val actual = evaluateForSuccess("val x = 1")
    assert(actual === "1")
  }

  test("evaluation should return stack trace when exception is thrown") {
    val (Failure(stackTrace), _) = repl.evaluate("sys.error(\"Error\")")

    stackTrace should startWith("java.lang.RuntimeException: Error")
  }

  test("evaluation should capture printlns") {
    val out = new StringBuffer
    repl.evaluate("""println("Hello, World!")""", out.append(_))
    assert(out.toString.trim === "Hello, World!")
  }

  test("evaluation should return incomplete") {
    val (result, _) = repl.evaluate("""foo(""")
    assert(result === Incomplete)
  }

  /** Facility no longer exists
  test("interrupt should stop current evaluation") {
    // CY: Not thrilled about the complexity/fragility of this unit test, but important
    // behavior to codify

    val start = new CountDownLatch(1)
    val complete = new CountDownLatch(1)

    ops.spawn {
      start.countDown()

      repl.evaluate(
        """while(true) {
          |  1 + 1
          |}""".stripMargin)

      complete.countDown()
    }

    start.await()
    Thread.sleep(5000)

    repl.interrupt()

    assert(complete.await(5, TimeUnit.SECONDS) === true)
  }
  **/

  test("completion should set matched text") {
    val (matchedText, matches) = repl.complete("com.bwa", 7)
    assert(matchedText === "bwa")
    assert(matches.head.matchedValue === "bwater")
  }

  test("objectInfo should return method arguments") {
    val scalaCompletion = repl.objectInfo("sys.error")
    assert(scalaCompletion === Seq("def error(message: String): Nothing"))

    // total random guess, but it looks like Scala saves the variable names, but Java doesn't

    val javaCompletion = repl.objectInfo("java.lang.StrictMath.hypot")
    assert(javaCompletion === Seq("def hypot(Double, Double): Double"))
  }

  test("objectInfo should return method overloads") {
    val completion = repl.objectInfo("Console.println")
    assert(completion === Seq("def println(): Unit", "def println(x: Any): Unit"))
  }

  def evaluateForSuccess(code: String) = {
    val (Success(Text(actual)), _) = repl.evaluate(code)
    actual
  }
}
