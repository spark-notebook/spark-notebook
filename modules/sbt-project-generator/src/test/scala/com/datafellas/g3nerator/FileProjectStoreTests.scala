package com.datafellas.g3nerator

import java.io.File
import java.nio.file.{Files, Paths}

import com.datafellas.g3nerator.FileUtils._
import com.datafellas.g3nerator.model._
import notebook.Notebook
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class FileProjectStoreTests extends WordSpec with Matchers with BeforeAndAfterAll {

  var tempDir: File = _
  var project: Project = _

  val snb = Await.result(Notebook.deserializeFuture(DataSamples.loadNotebook("generator-simple.snb")), 10 seconds)

  override def beforeAll(): Unit = {
    tempDir = Files.createTempDirectory("project-store-test").toFile

    val projectConfig = ProjectConfig(pkg = "pkg",
      version = "0.0.1",
      maintainer = "tester-maintainer",
      dockerRepo = "dockerRepo",
      dockercfg = "dockercfg",
      mesosVersion = "mesosVersion"
    )

    project =  new Project(snb = snb,
      projectDir = tempDir,
      initialConfig = projectConfig,
      dependencyConfig = DataSamples.CommonUrlConfig,
      productionConfig = None
    )
  }


  val sampleProjectConfig =
    """
      |[ { "pkg" : "pkg",
      |    "version" : "0.0.1",
      |    "maintainer" : "tester-maintainer",
      |    "dockerRepo" : "dockerRepo",
      |    "dockercfg" : "dockercfg",
      |    "mesosVersion" : "mesosVersion",
      |    "name" : "sample-project",
      |    "outputDirectory" : "sample-project"}
      |]
    """.stripMargin

  "A ProjectStore" should {
    "create a new empty store given an empty directory" in {
      val emptyDir = tempDir / "empty"
      val store = new FileProjectStore(emptyDir)
      store.list should be ('empty)
    }

    "create a new store using an existing definition file" in {
      val existingStoreDir = tempDir / "non-empty"
      val existingStore = existingStoreDir(FileProjectStore.StoreFile)
      FileUtils.writeTo(existingStore)(sampleProjectConfig)
      val store = new FileProjectStore(existingStoreDir)
      store.list.size should be (1)
    }

    "store a new project" in {
      val emptyDir = tempDir / "empty-2"
      val store = new FileProjectStore(emptyDir)
      store.add(project.config)
      store.list.size should be (1)
      store.list.head.name should be (Some("generator-simple"))
    }

    "support adding the same project twice" in {
      val emptyDir = tempDir / "empty-3"
      val store = new FileProjectStore(emptyDir)
      store.add(project.config)
      store.add(project.config)
      store.list.size should be (1)
    }

  }
}
