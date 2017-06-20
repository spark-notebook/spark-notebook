package repl

import notebook.kernel.repl.common.ReplT
import notebook.kernel.{Failure, Incomplete, Repl, Success}
import notebook.util.Match
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class ReplTests extends WordSpec with Matchers with BeforeAndAfterAll {

  var repl: ReplT = _
  override def beforeAll() = {
    repl = new Repl()
  }
  override def afterAll() = {
    repl.stop()
  }

  "ReplT can build Repl" in {
    ReplT.create(Nil, Nil)
  }

  "fail when attempting to use undefined code" in {
    repl.evaluate("val x = new NotDefinedClass") match {
      case ( Failure(_), _ ) => succeed
      case _ => fail("Expected Failure from evaluation")
    }
  }

  "fail with incomplete code" in {
    repl.evaluate("case class ReplTestClass(va") match {
      case ( Incomplete, _ ) => succeed
      case _ => fail("Expected Incomplete from evaluation")
    }
  }

  "successfully evaluate coplete code" in {
    repl.evaluate("case class ReplTestClass(val x:Int)") match {
      case ( Success(_), _ ) => succeed
      case _ => fail("Expected Success from evaluation")
    }
  }

  "return a type name of defined term" in {
    repl.evaluate("case class ReplTestClassGetTypeName(val x:Int); val x = new ReplTestClassGetTypeName(5)") match {
      case ( Success(_), _ ) =>
        repl.getTypeNameOfTerm("x") match {
          case None =>
            fail("Expected Some type name")
          case Some(typeName) =>
            typeName should endWith("ReplTestClassGetTypeName")
            succeed
        }
      case _ =>
        fail("Expected Success from evaluation")
    }
  }

  "do not render the last defined variable when cell returns nothing" in {
    repl.evaluate("val seq = Seq(1, 2, 3)") match {
      case ( Success(resultWidgets), _ ) =>
        resultWidgets.size shouldBe 0
        succeed
      case _ =>
        fail("Expected not to render the last defined variable")
    }
  }

  "render the return value of a cell (if one exists)" in {
    repl.evaluate("Seq(1, 2, 3)") match {
      case ( Success(resultWidgets), _ ) =>
        resultWidgets.size shouldBe 1
        succeed
      case _ =>
        fail("Expected to render the result value")
    }
  }

  "return object information for code" in {
    var code =
      """
        |case class ReplTestClassInformation(val prop:Int) {
        |  def dummy: Int = prop
        |}
        |val x = new ReplTestClassInformation(5)
      """.stripMargin
    repl.evaluate(code) match {
      case ( Success(_), _ ) =>
        repl.objectInfo("x.du", 4) shouldBe List("dummy")
        succeed
      case _ =>
        fail("Expected Success from evaluation")
    }
  }

  "return completed line for code" in {
    var code =
      """
        |case class ReplTestClassCompletion(val prop:Int) {
        |  def dummy: Int = prop
        |}
        |val x = new ReplTestClassCompletion(5)
      """.stripMargin
    repl.evaluate(code) match {
      case ( Success(_), _ ) =>
        var completed = repl.complete("x.du", 4)
        completed shouldBe ("du", List( Match("dummy", Map.empty[String, String]) ) )
        succeed
      case _ =>
        fail("Expected Success from evaluation")
    }
  }

  "adding a jar must keep defined code intact" in {
    if ( sys.env.contains("SKIP_WHEN_TRAVIS") ) {
      cancel(": Test skipped on TravisCI, causes OOM.")
    } else {
      val privRepl = new Repl()
      privRepl.evaluate("case class MyTestClassToKeep()")
      val replInit = privRepl.addCp(List.empty[String])
      val newRepl = replInit._1
      replInit._2.apply() // calls the replay logic on the new repl...
      val res = newRepl.evaluate("val x = new MyTestClassToKeep") match {
        case (Success(_), _) =>
          succeed
        case ex =>
          fail(s"Expected Success from evaluation but received $ex")
      }
      newRepl.stop()
      res
    }
  }

}
