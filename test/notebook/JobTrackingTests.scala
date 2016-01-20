package notebook

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JobTrackingTests extends Specification {
  "encodes jobGroup" in {
    JobTracking.jobGroupId(cellId = "abc") must beEqualTo("cell-abc")
  }

  "decodes cellId" in {
    JobTracking.toCellId(Option("cell-abc")) must beEqualTo(Some("abc"))
  }

  "encodes jobDescription" in {
    // must remove all special chars, especially " and \n, so job description can be enclosed inside " ".
    val code = """val x = sqlContext.sql("select * from users")
        |.collect()
        |.map { x: Row => s"$x\"" }""".stripMargin
    val expected = "run-1234567: val x = sqlContext.sql('select * from users').collect().map { x: Row = s'x'' }"
    JobTracking.jobDescription(
      cellCode = code,
      runId = 1234567) must beEqualTo(expected)
  }

  "decodes runId" in {
    JobTracking.getCellRunId(Option("run-1234567: val abc=rdd map x")) should beEqualTo(Some(1234567L))
  }
}
