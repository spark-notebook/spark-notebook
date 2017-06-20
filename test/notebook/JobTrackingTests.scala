package notebook

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class JobTrackingTests extends WordSpec with Matchers with BeforeAndAfterAll {
  "encodes jobGroup" in {
    JobTracking.jobGroupId(cellId = "abc") shouldBe "cell-abc"
  }

  "decodes cellId" in {
    JobTracking.toCellId(Option("cell-abc")) shouldBe Some("abc")
  }

  "encodes jobDescription" in {
    // must remove all special chars, especially " and \n, so job description can be enclosed inside " ".
    val code = """val x = sqlContext.sql("select * from users")
        |.collect()
        |.map { x: Row => s"$x\"" }""".stripMargin
    val expected = "run-1234567: val x = sqlContext.sql('select * from users').collect().map { x: Row = s'x'' }"
    JobTracking.jobDescription(
      cellCode = code,
      runId = 1234567) shouldBe expected
  }

  "decodes runId" in {
    JobTracking.getCellRunId(Option("run-1234567: val abc=rdd map x")) shouldBe Some(1234567L)
  }
}
