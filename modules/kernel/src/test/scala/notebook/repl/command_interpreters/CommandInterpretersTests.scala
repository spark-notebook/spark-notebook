package notebook.repl.command_interpreters

import akka.actor.Actor
import akka.event.Logging
import notebook.OutputTypes
import notebook.client.ExecuteRequest
import notebook.repl.ReplCommand
import org.scalatest._
import play.api.libs.json.{JsObject, JsString}

class CommandInterpretersTests extends FunSpec with Matchers with BeforeAndAfterAll {
  val QQQ = "\"\"\""

   describe ("Repl command interpreters") {
     val interpreters = combineIntepreters(defaultInterpreters)

     def matchCmd(code: String) = {
       interpreters(ExecuteRequest("cellid", 1, code))
     }

     it ("recognizes :sh") {
      val result = matchCmd(":sh ls -lh")
      result.replCommand.trim shouldBe
        s"""import sys.process._
          |println(s${QQQ}ls -lh${QQQ}.!!(ProcessLogger(out => (), err => println(err))))
          |()
        """.stripMargin.trim
       result.outputType shouldBe OutputTypes.`text/plain`
    }

     it ("recognizes :sql") {
       val result = matchCmd(":sql select * from table_name where key1 = value1")
       result.replCommand.trim shouldBe
         s"""
           |import notebook.front.widgets.Sql
           |import notebook.front.widgets.Sql._
           |new Sql(sqlContext, s${QQQ}select * from table_name where key1 = value1${QQQ})
           |""".stripMargin.trim
       result.outputType shouldBe OutputTypes.`text/html`
     }

     it("defaults to text/html for regular code/unknown commands"){
       val code = "val regularCode = 123"
       matchCmd(code) shouldBe ReplCommand(OutputTypes.`text/html`, code)
     }

     describe("removed commands") {
       val removed = Seq(":dp repo", ":local-repo repo", ":remote-repo repo", ":cp cp")
       removed.foreach { cmd =>
         it(s"prints a message on a removed command: '$cmd'") {
           matchCmd(cmd).replCommand should include("This command was removed")
         }
       }
     }

     describe("scala code result output type makers"){
       val scalaCode = "some_code()\ncode_returning_output_type()"

       val simpleOutTypes =  OutputTypeCommand.scalaResultMarkers
         .filterNot(outType => Seq("html", "svg").contains(outType.prefix))

       simpleOutTypes.foreach { case OutputTypeMarker(prefix, outputType) =>
         val result = matchCmd(s":${prefix} \n $scalaCode")
         it(s"recognizes :$prefix and indicates that code shall return $outputType output type)") {
           result.outputType shouldBe outputType
           result.replCommand.trim shouldBe scalaCode
         }
       }

       Seq(":svg", ":html").foreach { prefix =>
         it(s"recognizes $prefix, and transform strings to XML/HTML for displaying") {
           matchCmd(s":svg \n$scalaCode").replCommand should include(
             s"parseToXml({\n  $scalaCode\n})"
           )
         }
       }
     }
  }
}
