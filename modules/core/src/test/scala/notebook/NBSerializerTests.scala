package notebook

import notebook.NBSerializer.Metadata
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject}

import NBSerializer._

class NBSerializerTests extends WordSpec with Matchers with BeforeAndAfterAll {

  val testId = "foo-bar-loo-lar"
  val testName = "test-notebook-name"
  val sparkNotebook = Map("build" -> "unit-tests")
  val customLocalRepo = Some("local-repo")
  val customRepos = Some(List("custom-repo"))
  val customDeps = Some(List(""""org.custom" % "dependency" % "1.0.0""""))
  val customImports = Some(List("""import org.cusom.dependency.SomeClass"""))
  val customArgs = Some(List.empty[String])
  val customSparkConf = Some(JsObject( List(("spark.driverPort", JsNumber(1234))) ))
  val customVars = Some(Map( "HDFS_ROOT" -> "/tmp", "INTERNAL_DOCS" ->  "confidential"))

  val metadata = new Metadata(
    id = testId,
    name = testName,
    user_save_timestamp =  new DateTime(1999, 9, 9, 9, 9, 9, DateTimeZone.getDefault).toDate,
    auto_save_timestamp =  new DateTime(2001, 1, 1, 0, 0, 0, DateTimeZone.getDefault).toDate,
    sparkNotebook = Some(sparkNotebook),
    customLocalRepo = customLocalRepo,
    customRepos = customRepos,
    customDeps = customDeps,
    customImports = customImports,
    customArgs = customArgs,
    customSparkConf = customSparkConf,
    customVars = customVars
  )

  val notebookSer =
    """{
      |  "metadata" : {
      |    "id" : "foo-bar-loo-lar",
      |    "name" : "test-notebook-name",
      |    "user_save_timestamp" : "1999-09-09T09:09:09.000Z",
      |    "auto_save_timestamp" : "2001-01-01T00:00:00.000Z",
      |    "language_info" : {
      |      "name" : "scala",
      |      "file_extension" : "scala",
      |      "codemirror_mode" : "text/x-scala"
      |    },
      |    "trusted" : true,
      |    "sparkNotebook" : {
      |      "build" : "unit-tests"
      |    },
      |    "customLocalRepo" : "local-repo",
      |    "customRepos" : [ "custom-repo" ],
      |    "customDeps" : [ "\"org.custom\" % \"dependency\" % \"1.0.0\"" ],
      |    "customImports" : [ "import org.cusom.dependency.SomeClass" ],
      |    "customArgs" : [ ],
      |    "customSparkConf" : {
      |      "spark.driverPort" : 1234
      |    },
      |    "customVars" : {
      |      "HDFS_ROOT" : "/tmp",
      |      "INTERNAL_DOCS" : "confidential"
      |    }
      |  },
      |  "cells" : [ ]
      |}""".stripMargin

  val notebookWithContent = Notebook(Some(metadata), nbformat = None)

  "Notebook" should {
    "serialize a notebook as a valid JSON" in {
      val nb = Notebook.serialize(notebookWithContent)
      nb shouldBe notebookSer
    }

    "deserialize a json encoded notebook as a valid object" in {
      val ser = Notebook.deserialize(notebookSer)
      ser shouldBe Some(notebookWithContent)
    }

    "fail to deserialize an empty Json" in {
      an [NotebookDeserializationError] should be thrownBy {
        Notebook.deserialize("{}")
      }
    }

    "fail to deserialize invalid Json" in {
      an [NotebookDeserializationError] should be thrownBy {
        Notebook.deserialize("{:=")
      }
    }

    "serialize to a diff-friendly format" in {
      val origNotebook = Notebook(Some(metadata), nbformat = None)
      val deltaNotebook = Notebook(Some(metadata.copy(customLocalRepo = Some("other-local-repo"))), nbformat = None)

      val origNbSer = Notebook.serialize(origNotebook)
      val deltaNbSer = Notebook.serialize(deltaNotebook)

      val toLines: String => Array[String] = s => s.split("\n")

      val nbDiff = toLines(deltaNbSer).toSet diff toLines(origNbSer).toSet
      nbDiff.head should include ("other-local-repo")
      nbDiff.head should not include ("metadata") // to exclude that the diff is the whole doc
    }

    "be able to read older snb formats" when {
      val oldFormatNotebookSer =
        """{
          |  "metadata" : {
          |    "name" : "test-notebook-name",
          |    "user_save_timestamp" : "1999-09-09T09:09:09.000Z",
          |    "auto_save_timestamp" : "2001-01-01T00:00:00.000Z"
          |  }
          |}""".stripMargin
      val oldDeserialized = Notebook.deserialize(oldFormatNotebookSer)

      "notebook.metadata.id was undefined -> it gets a new random UUID" in {
        oldDeserialized.get.metadata.get.id.length shouldBe 36
      }
      "notebook.cells is undefined -> it becomes an empty list" in {
        oldDeserialized.get.cells.get shouldBe List.empty
      }
    }
  }
}
