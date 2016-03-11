package utils

import org.specs2.mutable.Specification
import org.specs2.specification.{Fragments, Step}

trait BeforeAllAfterAll extends Specification {
  // see http://bit.ly/11I9kFM (specs2 User Guide)
  override def map(fragments: =>Fragments) =
    Step(beforeAll) ^ fragments ^ Step(afterAll)

  protected def beforeAll()
  protected def afterAll()
}
