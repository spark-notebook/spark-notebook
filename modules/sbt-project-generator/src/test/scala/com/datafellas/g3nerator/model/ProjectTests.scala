package com.datafellas.g3nerator.model

import java.io.File
import java.nio.file.{Files, Paths}

import com.datafellas.g3nerator.{DataSamples, Job}
import com.typesafe.config.ConfigFactory
import notebook.NBSerializer.CodeCell
import notebook.Notebook

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.{BeforeAndAfterAll, Matchers, OptionValues, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

class ProjectTests extends WordSpec with Matchers with BeforeAndAfterAll with OptionValues  {

  var tempDir:File = _
  var project: Project = _

  override def beforeAll(): Unit = {
    tempDir =  Files.createTempDirectory("project-test").toFile

    project =  new Project(snb = snb,
      projectDir = tempDir,
      initialConfig = projectConfig,
      dependencyConfig = DataSamples.CommonUrlConfig,
      productionConfig = None
    )
  }

  // notebook name in metadata: generator-output-simple-parquet
  val notebook = DataSamples.loadNotebook("generator-simple.snb")

  val snb = Await.result(Notebook.deserializeFuture(notebook), 10 seconds)

  val projectConfig = ProjectConfig(pkg = "pkg",
    version = "0.0.1",
    maintainer = "tester-maintainer",
    dockerRepo = "dockerRepo",
    dockercfg = "dockercfg",
    mesosVersion = "mesosVersion"
  )

  "a Project" should {

    "generate a job" in {
      println("Job temp dir:" + tempDir)
      val job: Job = project.mkJob
      job.root.getAbsolutePath should startWith (tempDir.getAbsolutePath)

      job.`root/build.sbt`.getAbsolutePath should startWith (tempDir.getAbsolutePath)
      job.`root/build.sbt`.getAbsolutePath should endWith ("build.sbt")
      job.generateOnly(local=true)

      job.`root/src/main/resources/application.conf`.exists() should be (true)
      // generated configuration should be valid
      val appConf = Source.fromFile(job.`root/src/main/resources/application.conf`).getLines().mkString("\n")
      val conf = try {
        ConfigFactory.parseString(appConf)
      } catch {
        case t:Throwable => fail("failed to parse generated configuration:" + t.getMessage, t)
      }
      val customVars = conf.getConfig(Job.NotebookVarsPrefix)
      // these are fragile tests if we change the test data
      customVars.entrySet().size() should be (2)
      customVars.getString("HDFS_ROOT") should be ("/tmp")
    }

  }
  "A test" should {
    "print the dir" in {
      println(" Using test dir:" + tempDir)
    }
  }

}


