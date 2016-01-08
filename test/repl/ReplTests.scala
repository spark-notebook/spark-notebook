package repl

import notebook.kernel.repl.common.ReplT
import notebook.kernel.{Failure, Incomplete, Success, Repl}
import notebook.util.Match
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ReplTests extends Specification with BeforeAfter {

  var repl: ReplT = _
  def before = repl = new Repl()
  def after = repl.stop()

  "fail when attempting to use undefined code" in {
    repl.evaluate("val x = new NotDefinedClass") match {
      case ( Failure(_), _ ) => success
      case _ => failure("Expected Failure from evaluation")
    }
  }

  "fail with incomplete code" in {
    repl.evaluate("case class ReplTestClass(va") match {
      case ( Incomplete, _ ) => success
      case _ => failure("Expected Incomplete from evaluation")
    }
  }

  "successfully evaluate coplete code" in {
    repl.evaluate("case class ReplTestClass(val x:Int)") match {
      case ( Success(_), _ ) => success
      case _ => failure("Expected Success from evaluation")
    }
  }

  "return a type name of defined term" in {
    repl.evaluate("case class ReplTestClass(val x:Int); val x = new ReplTestClass(5)") match {
      case ( Success(_), _ ) =>
        repl.getTypeNameOfTerm("x") match {
          case None =>
            failure("Expected Some type name")
          case Some(typeName) =>
            typeName must beEqualTo("ReplTestClass")
            success
        }
      case _ =>
        failure("Expected Success from evaluation")
    }
  }

  "return object information for code" in {
    var code =
      """
        |case class ReplTestClass(val prop:Int) {
        |  def dummy: Int = prop
        |}
        |val x = new ReplTestClass(5)
      """.stripMargin
    repl.evaluate(code) match {
      case ( Success(_), _ ) =>
        repl.objectInfo("x.du", 4) must beEqualTo( List("dummy") )
        success
      case _ =>
        failure("Expected Success from evaluation")
    }
  }

  "return completed line for code" in {
    var code =
      """
        |case class ReplTestClass(val prop:Int) {
        |  def dummy: Int = prop
        |}
        |val x = new ReplTestClass(5)
      """.stripMargin
    repl.evaluate(code) match {
      case ( Success(_), _ ) =>
        var completed = repl.complete("x.du", 4)
        completed must beEqualTo( ("du", List( Match("dummy", Map.empty[String, String]) ) ) )
        success
      case _ =>
        failure("Expected Success from evaluation")
    }
  }

  "adding a jar must keep defined code intact" in {
    val privRepl = new Repl()
    privRepl.evaluate("case class MyTestClass()")
    val newRepl = privRepl.addCp(List.empty[String])._1
    val res = newRepl.evaluate("val x = new MyTestClass") match {
      case ( Success(_), _ ) =>
        success
      case ex =>
        failure(s"Expected Success from evaluation but received $ex")
    }
    newRepl.stop()
    res
  }

}
