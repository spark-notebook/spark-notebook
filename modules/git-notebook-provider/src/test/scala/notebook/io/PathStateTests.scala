package notebook.io

import java.nio.file.{Files, Path}

import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}


class PathStateTests extends WordSpec with Matchers with BeforeAndAfterAll {

  var testDir : Path = _
  var emptyDir : Path = _
  var nonExistingDir : Path = _
  var nonEmptyDir : Path = _
  var nonEmptyGitDir : Path = _

  override def beforeAll: Unit = {

    testDir = Files.createTempDirectory("path-state-test")

    def mkdir(relPath:String) : Path = {
      val path = testDir.resolve(relPath)
      Files.createDirectory(path)
      path
    }

    emptyDir = mkdir("empty")
    nonExistingDir = testDir.resolve("non-exists")
    nonEmptyDir = mkdir("non-empty")
    nonEmptyGitDir = mkdir("non-empty-git")

    val someFile = nonEmptyDir.resolve("file.txt")
    Files.write(someFile, "SomeData".getBytes("UTF-8"))

    val gitDir = nonEmptyGitDir.resolve(".git")
    Files.createDirectory(gitDir)
  }

  override def afterAll: Unit = {
    FileUtils.deleteDirectory( testDir.toFile )
  }

  "Path State" should {

    "resolve an empty dir" in  {
      val emptyPath = PathState(emptyDir.toFile.getAbsolutePath)
      emptyPath should be ('success)
      emptyPath.get shouldBe an [EmptyPath]
    }

    "resolve a non-existing dir and create it" in {
      val nonExitingPath = PathState(nonExistingDir.toFile.getAbsolutePath)
      nonExitingPath should be ('success)
      nonExitingPath.get shouldBe an [EmptyPath]
      nonExistingDir.toFile.exists() should be (true)
    }

    "resolve an existing dir with data" in {
      val nonEmptyPath = PathState(nonEmptyDir.toFile.getAbsolutePath)
      nonEmptyPath should be ('success)
      nonEmptyPath.get shouldBe an [NonEmptyNonGitPath]
    }

    "resolve an existing git with data" in {
      val gitPath = PathState(nonEmptyGitDir.toFile.getAbsolutePath)
      gitPath  should be ('success)
      gitPath.get shouldBe an [NonEmptyGitPath]
    }

  }

}
